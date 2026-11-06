package com.algorycarservice.market.service;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.common.schedule.SlotCalendar;
import com.algorycarservice.market.dto.BookingRequest;
import com.algorycarservice.market.dto.BookingResponse;
import com.algorycarservice.market.dto.BookingStatusRequest;
import com.algorycarservice.market.model.Booking;
import com.algorycarservice.market.model.BookingStatus;
import com.algorycarservice.market.model.Business;
import com.algorycarservice.market.repository.BookingRepository;
import com.algorycarservice.user.model.User;
import com.algorycarservice.user.repository.UserRepository;
import com.algorycarservice.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    /** What the business may turn a booking into, from each open state. */
    private static final Map<BookingStatus, Set<BookingStatus>> PROVIDER_TRANSITIONS = Map.of(
            BookingStatus.REQUESTED, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.REJECTED),
            BookingStatus.CONFIRMED, EnumSet.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED));

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BusinessService businessService;
    private final SlotCalendar slotCalendar;

    /** A customer asks for a slot; it holds the slot until the business answers. */
    @Transactional
    public BookingResponse create(Long customerId, BookingRequest request) {
        Business business = businessService.getActive(request.businessId());
        if (!business.getCategories().contains(request.category())) {
            throw new ApiException(ErrorCode.SERVICE_NOT_OFFERED);
        }
        if (slotCalendar.isClosed(request.date())) {
            throw new ApiException(ErrorCode.APPOINTMENT_CLOSED_DAY);
        }
        if (!slotCalendar.isSlot(request.startTime())) {
            throw new ApiException(ErrorCode.APPOINTMENT_INVALID_TIME);
        }
        if (!slotCalendar.isUpcoming(request.date(), request.startTime())) {
            throw new ApiException(ErrorCode.APPOINTMENT_IN_PAST);
        }
        String plate = VehicleService.normalizePlate(request.plate());
        List<Booking> sameDay = bookingRepository.findAllByBusinessIdAndDateAndStatusIn(
                business.getId(), request.date(), BookingStatus.ACTIVE);
        if (sameDay.stream().anyMatch(booking -> booking.getPlate().equals(plate))) {
            throw new ApiException(ErrorCode.APPOINTMENT_DUPLICATE);
        }
        long inSlot = sameDay.stream().filter(booking -> booking.getStartTime().equals(request.startTime())).count();
        if (inSlot >= business.getSlotCapacity()) {
            throw new ApiException(ErrorCode.APPOINTMENT_SLOT_FULL);
        }
        User customer = userRepository.getReferenceById(customerId);
        Booking booking = bookingRepository.save(Booking.builder()
                .business(business)
                .customer(customer)
                .category(request.category())
                .date(request.date())
                .startTime(request.startTime())
                .plate(plate)
                .vehicle(clean(request.vehicle()))
                .note(clean(request.note()))
                .status(BookingStatus.REQUESTED)
                .build());
        return toResponse(booking);
    }

    public List<BookingResponse> mine(Long customerId) {
        return bookingRepository.findAllByCustomerIdOrderByDateDescStartTimeDesc(customerId).stream()
                .map(this::toResponse).toList();
    }

    /** The customer withdraws a booking that is still open and has not started yet. */
    @Transactional
    public BookingResponse cancel(Long customerId, Long id, String note) {
        Booking booking = bookingRepository.findById(id)
                .filter(candidate -> candidate.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("booking"));
        if (!booking.getStatus().isActive() || !slotCalendar.isUpcoming(booking.getDate(), booking.getStartTime())) {
            throw new ApiException(ErrorCode.APPOINTMENT_STATUS_INVALID);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStatusNote(clean(note));
        return toResponse(booking);
    }

    /** The business's bookings; open ones by default, soonest first. */
    public List<BookingResponse> forBusiness(Long ownerId, Set<BookingStatus> statuses) {
        Business business = businessService.getOwned(ownerId);
        Set<BookingStatus> filter = statuses == null || statuses.isEmpty() ? BookingStatus.ACTIVE : statuses;
        return bookingRepository.findAllByBusinessIdAndStatusInOrderByDateAscStartTimeAsc(business.getId(), filter)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse changeStatus(Long ownerId, Long id, BookingStatusRequest request) {
        Business business = businessService.getOwned(ownerId);
        Booking booking = bookingRepository.findById(id)
                .filter(candidate -> candidate.getBusiness().getId().equals(business.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("booking"));
        if (!PROVIDER_TRANSITIONS.getOrDefault(booking.getStatus(), Set.of()).contains(request.status())) {
            throw new ApiException(ErrorCode.APPOINTMENT_STATUS_INVALID);
        }
        booking.setStatus(request.status());
        booking.setStatusNote(clean(request.note()));
        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.from(booking, booking.getStartTime().plus(slotCalendar.slotLength()));
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
