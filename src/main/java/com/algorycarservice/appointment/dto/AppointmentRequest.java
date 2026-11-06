package com.algorycarservice.appointment.dto;

import com.algorycarservice.appointment.model.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @param vehicleId registered vehicle, if any; without it a vehicle with the same plate is linked automatically
 * @param startTime must be one of the slots returned by {@code GET /appointments/slots}
 */
public record AppointmentRequest(
        Long vehicleId,

        @NotBlank(message = "{validation.plate.required}")
        @Size(max = 16, message = "{validation.plate.size}")
        String plate,

        @NotBlank(message = "{validation.customer-name.required}")
        @Size(max = 100, message = "{validation.customer-name.size}")
        String customerName,

        @Size(max = 20, message = "{validation.phone.size}")
        String customerPhone,

        @NotNull(message = "{validation.appointment-type.required}")
        AppointmentType type,

        @NotNull(message = "{validation.date.required}")
        LocalDate date,

        @NotNull(message = "{validation.time.required}")
        LocalTime startTime,

        @Size(max = 500, message = "{validation.note.size}")
        String notes
) {
}
