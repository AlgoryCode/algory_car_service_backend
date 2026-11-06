package com.algorycarservice.appointment.dto;

import com.algorycarservice.appointment.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentStatusRequest(
        @NotNull(message = "{validation.status.required}")
        AppointmentStatus status,

        @Size(max = 300, message = "{validation.note.size}")
        String note
) {
}
