package com.algorycarservice.market.dto;

import com.algorycarservice.market.model.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequest(
        @NotNull(message = "{validation.business.required}")
        Long businessId,

        @NotNull(message = "{validation.category.required}")
        ServiceCategory category,

        @NotNull(message = "{validation.date.required}")
        LocalDate date,

        @NotNull(message = "{validation.time.required}")
        LocalTime startTime,

        @NotBlank(message = "{validation.plate.required}")
        @Size(max = 16, message = "{validation.plate.size}")
        String plate,

        @Size(max = 100, message = "{validation.vehicle-info.size}")
        String vehicle,

        @Size(max = 500, message = "{validation.note.size}")
        String note
) {
}
