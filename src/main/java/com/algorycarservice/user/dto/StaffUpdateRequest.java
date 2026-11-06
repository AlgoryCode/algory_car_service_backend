package com.algorycarservice.user.dto;

import com.algorycarservice.user.model.Role;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param newPassword optional reset by the admin; blank keeps the current password */
public record StaffUpdateRequest(
        @NotBlank(message = "{validation.full-name.required}")
        @Size(max = 100, message = "{validation.full-name.size}")
        String fullName,

        @Size(max = 20, message = "{validation.phone.size}")
        String phone,

        @Size(max = 60, message = "{validation.position.size}")
        String position,

        @NotNull(message = "{validation.role.required}")
        Role role,

        @Min(value = 0, message = "{validation.leave-days.range}")
        @Max(value = 60, message = "{validation.leave-days.range}")
        int annualLeaveDays,

        boolean active,

        @Size(max = 72, message = "{validation.password.size}")
        String newPassword
) {
}
