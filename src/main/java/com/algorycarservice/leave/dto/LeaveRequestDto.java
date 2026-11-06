package com.algorycarservice.leave.dto;

import com.algorycarservice.leave.model.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** @param userId admins may file a leave for someone else; it is approved right away */
public record LeaveRequestDto(
        @NotNull(message = "{validation.leave-type.required}")
        LeaveType type,

        @NotNull(message = "{validation.date.required}")
        LocalDate startDate,

        @NotNull(message = "{validation.date.required}")
        LocalDate endDate,

        @Size(max = 300, message = "{validation.note.size}")
        String reason,

        Long userId
) {
}
