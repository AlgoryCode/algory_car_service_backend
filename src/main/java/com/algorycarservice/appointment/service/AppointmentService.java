package com.algorycarservice.appointment.service;

import com.algorycarservice.appointment.dto.AppointmentRequest;
import com.algorycarservice.appointment.dto.AppointmentResponse;
import com.algorycarservice.appointment.dto.AppointmentStatusRequest;
import com.algorycarservice.appointment.dto.SlotsResponse;
import com.algorycarservice.appointment.model.Appointment;
import com.algorycarservice.appointment.model.AppointmentStatus;
import com.algorycarservice.appointment.repository.AppointmentRepository;
import com.algorycarservice.common.config.AppProperties;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.common.i18n.Messages;
import com.algorycarservice.common.schedule.SlotCalendar;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.user.service.StaffService;
import com.algorycarservice.vehicle.model.Vehicle;
import com.algorycarservice.vehicle.repository.VehicleRepository;
import com.algorycarservice.vehicle.service.VehicleService;
import com.algorycarservice.workorder.dto.WorkOrderRequest;
import com.algorycarservice.workorder.dto.WorkOrderResponse;
import com.algorycarservice.workorder.model.WorkOrderStatus;
import com.algorycarservice.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    /** Lists default to the coming month and never span more than about a quarter. */
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 92;
    private static final long NO_ID = -1L;

    private final AppointmentRepository repository;
    private final VehicleService vehicleService;
    private final VehicleRepository vehicleRepository;
    private final StaffService staffService;
    private final WorkOrderService workOrderService;
    private final AppProperties properties;
    private final SlotCalendar slotCalendar;
    private final Messages messages;

    public List<AppointmentResponse> list(LocalDate from, LocalDate to, Set<AppointmentStatus> statuses) {
        LocalDate start = from != null ? from : today();
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_RANGE_DAYS);
        if (end.isAfter(start.plusDays(MAX_RANGE_DAYS))) {
            end = start.plusDays(MAX_RANGE_DAYS);
        }
        Set<AppointmentStatus> filter = statuses == null || statuses.isEmpty()
                ? EnumSet.allOf(AppointmentStatus.class) : statuses;
        return repository.findInRange(start, end, filter).stream().map(this::toResponse).toList();
    }

    /** Today's bookings still to come in, for the dashboard. */
    public List<AppointmentResponse> forToday() {
        LocalDate today = today();
        return repository.findInRange(today, today, AppointmentStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    public AppointmentResponse get(Long id) {
        return toResponse(getAppointment(id));
    }

    public SlotsResponse slots(LocalDate date) {
        AppProperties.Appointments config = properties.appointments();
        if (isClosed(date)) {
            return new SlotsResponse(date, true, config.slotCapacity(), List.of());
        }
        List<Appointment> booked = repository.findOnDay(date, AppointmentStatus.ACTIVE, NO_ID);
        List<SlotsResponse.Slot> slots = slotTimes().stream().map(time -> {
            int count = (int) booked.stream().filter(appointment -> appointment.getStartTime().equals(time)).count();
            boolean available = count < config.slotCapacity() && isUpcoming(date, time);
            return new SlotsResponse.Slot(time, count, available);
        }).toList();
        return new SlotsResponse(date, false, config.slotCapacity(), slots);
    }

    /** Staff file a request for an admin to confirm; an admin's booking is confirmed right away. */
    @Transactional
    public AppointmentResponse create(AuthUser actor, AppointmentRequest request) {
        String plate = VehicleService.normalizePlate(request.plate());
        checkSlot(request.date(), request.startTime(), plate, NO_ID, true);
        Appointment appointment = Appointment.builder()
                .status(actor.isAdmin() ? AppointmentStatus.CONFIRMED : AppointmentStatus.REQUESTED)
                .createdBy(staffService.getUser(actor.id()))
                .build();
        apply(appointment, request, plate);
        return toResponse(repository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        requireActive(appointment);
        String plate = VehicleService.normalizePlate(request.plate());
        boolean moved = !appointment.getDate().equals(request.date())
                || !appointment.getStartTime().equals(request.startTime());
        checkSlot(request.date(), request.startTime(), plate, id, moved);
        apply(appointment, request, plate);
        return toResponse(appointment);
    }

    /** Only admins confirm requests; anyone may cancel, close or mark a no-show while the booking is open. */
    @Transactional
    public AppointmentResponse changeStatus(AuthUser actor, Long id, AppointmentStatusRequest request) {
        Appointment appointment = getAppointment(id);
        AppointmentStatus target = request.status();
        if (!appointment.getStatus().isActive() || target == AppointmentStatus.REQUESTED
                || target == appointment.getStatus()) {
            throw new ApiException(ErrorCode.APPOINTMENT_STATUS_INVALID);
        }
        if (target == AppointmentStatus.CONFIRMED && !actor.isAdmin()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        appointment.setStatus(target);
        appointment.setStatusNote(clean(request.note()));
        return toResponse(appointment);
    }

    /**
     * The customer has arrived: opens a work order for the vehicle and closes the appointment. The car has to be
     * registered first; one with the same plate is picked up even if it was added after the booking.
     */
    @Transactional
    public AppointmentResponse startWorkOrder(Long id) {
        Appointment appointment = getAppointment(id);
        requireActive(appointment);
        Vehicle vehicle = appointment.getVehicle() != null
                ? appointment.getVehicle()
                : vehicleRepository.findByPlate(appointment.getPlate())
                        .orElseThrow(() -> new ApiException(ErrorCode.APPOINTMENT_VEHICLE_REQUIRED));
        String complaint = messages.get("appointment.type." + appointment.getType().name())
                + (appointment.getNotes() != null ? " – " + appointment.getNotes() : "");
        WorkOrderResponse order = workOrderService.create(new WorkOrderRequest(vehicle.getId(), WorkOrderStatus.OPEN,
                truncate(complaint, 500), null, null, null, today(), List.of()));
        appointment.setVehicle(vehicle);
        appointment.setWorkOrderId(order.id());
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointment);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getAppointment(id));
    }

    private void apply(Appointment appointment, AppointmentRequest request, String plate) {
        Vehicle vehicle = request.vehicleId() != null
                ? vehicleService.getVehicle(request.vehicleId())
                : vehicleRepository.findByPlate(plate).orElse(null);
        appointment.setVehicle(vehicle);
        appointment.setPlate(vehicle != null ? vehicle.getPlate() : plate);
        appointment.setCustomerName(request.customerName().trim());
        appointment.setCustomerPhone(clean(request.customerPhone()));
        appointment.setType(request.type());
        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setNotes(clean(request.notes()));
    }

    private void checkSlot(LocalDate date, LocalTime time, String plate, Long excludeId, boolean mustBeUpcoming) {
        if (isClosed(date)) {
            throw new ApiException(ErrorCode.APPOINTMENT_CLOSED_DAY);
        }
        if (!slotTimes().contains(time)) {
            throw new ApiException(ErrorCode.APPOINTMENT_INVALID_TIME);
        }
        if (mustBeUpcoming && !isUpcoming(date, time)) {
            throw new ApiException(ErrorCode.APPOINTMENT_IN_PAST);
        }
        List<Appointment> sameDay = repository.findOnDay(date, AppointmentStatus.ACTIVE, excludeId);
        if (sameDay.stream().anyMatch(appointment -> appointment.getPlate().equals(plate))) {
            throw new ApiException(ErrorCode.APPOINTMENT_DUPLICATE);
        }
        long inSlot = sameDay.stream().filter(appointment -> appointment.getStartTime().equals(time)).count();
        if (inSlot >= properties.appointments().slotCapacity()) {
            throw new ApiException(ErrorCode.APPOINTMENT_SLOT_FULL);
        }
    }

    private List<LocalTime> slotTimes() {
        return slotCalendar.slotTimes();
    }

    private boolean isClosed(LocalDate date) {
        return slotCalendar.isClosed(date);
    }

    private boolean isUpcoming(LocalDate date, LocalTime time) {
        return slotCalendar.isUpcoming(date, time);
    }

    private LocalDate today() {
        return slotCalendar.today();
    }

    private static void requireActive(Appointment appointment) {
        if (!appointment.getStatus().isActive()) {
            throw new ApiException(ErrorCode.APPOINTMENT_STATUS_INVALID);
        }
    }

    private Appointment getAppointment(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("appointment"));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.from(appointment, appointment.getStartTime().plus(properties.appointments().slotLength()));
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
