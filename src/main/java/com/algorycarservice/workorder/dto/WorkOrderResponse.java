package com.algorycarservice.workorder.dto;

import com.algorycarservice.user.dto.UserRef;
import com.algorycarservice.vehicle.dto.VehicleRef;
import com.algorycarservice.workorder.model.ItemKind;
import com.algorycarservice.workorder.model.WorkOrder;
import com.algorycarservice.workorder.model.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WorkOrderResponse(
        Long id,
        VehicleRef vehicle,
        WorkOrderStatus status,
        String complaint,
        String notes,
        Integer mileageKm,
        UserRef technician,
        LocalDate openedOn,
        LocalDate completedOn,
        LocalDate paidOn,
        List<WorkOrderItemDto> items,
        BigDecimal partsTotal,
        BigDecimal laborTotal,
        BigDecimal total
) {

    public static WorkOrderResponse from(WorkOrder order) {
        List<WorkOrderItemDto> items = order.getItems().stream().map(WorkOrderItemDto::from).toList();
        BigDecimal parts = sum(items, ItemKind.PART);
        BigDecimal labor = sum(items, ItemKind.LABOR);
        return new WorkOrderResponse(order.getId(), VehicleRef.from(order.getVehicle()), order.getStatus(),
                order.getComplaint(), order.getNotes(), order.getMileageKm(), UserRef.from(order.getTechnician()),
                order.getOpenedOn(), order.getCompletedOn(), order.getPaidOn(), items, parts, labor, parts.add(labor));
    }

    private static BigDecimal sum(List<WorkOrderItemDto> items, ItemKind kind) {
        return items.stream()
                .filter(item -> item.kind() == kind)
                .map(WorkOrderItemDto::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
