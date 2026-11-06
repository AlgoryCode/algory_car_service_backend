package com.algorycarservice.workorder.dto;

import com.algorycarservice.workorder.model.WorkOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** @param vehicleId fixed after creation; updates ignore it */
public record WorkOrderRequest(
        @NotNull(message = "{validation.vehicle.required}")
        Long vehicleId,

        @NotNull(message = "{validation.status.required}")
        WorkOrderStatus status,

        @NotBlank(message = "{validation.complaint.required}")
        @Size(max = 500, message = "{validation.complaint.size}")
        String complaint,

        @Size(max = 1000, message = "{validation.note.size}")
        String notes,

        @Min(value = 0, message = "{validation.mileage.range}")
        @Max(value = 3_000_000, message = "{validation.mileage.range}")
        Integer mileageKm,

        Long technicianId,

        @NotNull(message = "{validation.date.required}")
        LocalDate openedOn,

        @NotNull
        @Size(max = 100, message = "{validation.items.size}")
        List<@Valid WorkOrderItemDto> items
) {
}
