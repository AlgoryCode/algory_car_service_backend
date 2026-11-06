package com.algorycarservice.workorder.service;

import com.algorycarservice.common.config.AppProperties;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.finance.service.FinanceService;
import com.algorycarservice.user.service.StaffService;
import com.algorycarservice.vehicle.service.VehicleService;
import com.algorycarservice.workorder.dto.PaymentRequest;
import com.algorycarservice.workorder.dto.WorkOrderItemDto;
import com.algorycarservice.workorder.dto.WorkOrderRequest;
import com.algorycarservice.workorder.dto.WorkOrderResponse;
import com.algorycarservice.workorder.model.WorkOrder;
import com.algorycarservice.workorder.model.WorkOrderStatus;
import com.algorycarservice.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final VehicleService vehicleService;
    private final StaffService staffService;
    private final FinanceService financeService;
    private final AppProperties properties;

    /** @param statuses empty means every status */
    public List<WorkOrderResponse> list(Set<WorkOrderStatus> statuses) {
        Set<WorkOrderStatus> filter = statuses == null || statuses.isEmpty()
                ? EnumSet.allOf(WorkOrderStatus.class)
                : statuses;
        return workOrderRepository.findAllByStatusInOrderByOpenedOnDescIdDesc(filter).stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    public List<WorkOrderResponse> listForVehicle(Long vehicleId) {
        vehicleService.getVehicle(vehicleId);
        return workOrderRepository.findAllByVehicleIdOrderByOpenedOnDescIdDesc(vehicleId).stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    public List<WorkOrderResponse> recent() {
        return workOrderRepository.findTop5ByOrderByCreatedAtDesc().stream().map(WorkOrderResponse::from).toList();
    }

    public long count(WorkOrderStatus status) {
        return workOrderRepository.countByStatus(status);
    }

    public WorkOrderResponse get(Long id) {
        return WorkOrderResponse.from(getOrder(id));
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder order = WorkOrder.builder().vehicle(vehicleService.getVehicle(request.vehicleId())).build();
        apply(order, request);
        return WorkOrderResponse.from(workOrderRepository.save(order));
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderRequest request) {
        WorkOrder order = getOrder(id);
        apply(order, request);
        if (order.getPaidOn() != null) {
            financeService.updateWorkOrderPayment(order.getId(), order.total());
        }
        return WorkOrderResponse.from(order);
    }

    @Transactional
    public WorkOrderResponse changeStatus(Long id, WorkOrderStatus status) {
        WorkOrder order = getOrder(id);
        setStatus(order, status);
        return WorkOrderResponse.from(order);
    }

    /** Collecting the payment books the work order total as service income. */
    @Transactional
    public WorkOrderResponse markPaid(Long id, PaymentRequest request) {
        WorkOrder order = getOrder(id);
        if (order.getPaidOn() != null) {
            throw new ApiException(ErrorCode.WORK_ORDER_ALREADY_PAID);
        }
        order.setPaidOn(request.paidOn());
        String description = order.getVehicle().getPlate() + " · " + order.getComplaint();
        financeService.recordWorkOrderPayment(order.getId(), order.total(), request.paidOn(), truncate(description, 200));
        return WorkOrderResponse.from(order);
    }

    @Transactional
    public WorkOrderResponse cancelPayment(Long id) {
        WorkOrder order = getOrder(id);
        if (order.getPaidOn() == null) {
            throw new ApiException(ErrorCode.WORK_ORDER_NOT_PAID);
        }
        order.setPaidOn(null);
        financeService.removeWorkOrderPayment(order.getId());
        return WorkOrderResponse.from(order);
    }

    /** Collected payments stay in the books; only the job itself is removed. */
    @Transactional
    public void delete(Long id) {
        workOrderRepository.delete(getOrder(id));
    }

    private WorkOrder getOrder(Long id) {
        return workOrderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("work-order"));
    }

    private void apply(WorkOrder order, WorkOrderRequest request) {
        order.setComplaint(request.complaint().trim());
        order.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
        order.setMileageKm(request.mileageKm());
        order.setTechnician(request.technicianId() == null ? null : staffService.getUser(request.technicianId()));
        order.setOpenedOn(request.openedOn());
        order.replaceItems(request.items().stream().map(WorkOrderItemDto::toItem).toList());
        setStatus(order, request.status());
    }

    /** Remembers the day the job was first finished; reopening it clears that date. */
    private void setStatus(WorkOrder order, WorkOrderStatus status) {
        order.setStatus(status);
        if (status.isFinished() && order.getCompletedOn() == null) {
            order.setCompletedOn(LocalDate.now(properties.timeZone()));
        } else if (!status.isFinished()) {
            order.setCompletedOn(null);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
