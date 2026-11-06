package com.algorycarservice.auth.dto;

import com.algorycarservice.user.dto.UserResponse;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String refreshToken,
        UserResponse user
) {
}
