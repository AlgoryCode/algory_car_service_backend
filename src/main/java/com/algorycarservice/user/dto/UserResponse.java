package com.algorycarservice.user.dto;

import com.algorycarservice.user.model.Role;
import com.algorycarservice.user.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        Long id,
        UUID businessUuid,
        String email,
        String fullName,
        String phone,
        String position,
        Role role,
        int annualLeaveDays,
        boolean active,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getBusinessUuid(), user.getEmail(), user.getFullName(), user.getPhone(), user.getPosition(),
                user.getRole(), user.getAnnualLeaveDays(), user.isActive(), user.getCreatedAt());
    }
}
