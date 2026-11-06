package com.algorycarservice.auth.service;

import com.algorycarservice.auth.dto.AuthResponse;
import com.algorycarservice.auth.dto.ChangePasswordRequest;
import com.algorycarservice.auth.dto.LoginRequest;
import com.algorycarservice.auth.dto.RefreshTokenRequest;
import com.algorycarservice.auth.dto.RegisterRequest;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.market.dto.CustomerRegisterRequest;
import com.algorycarservice.market.dto.ProviderRegisterRequest;
import com.algorycarservice.market.service.BusinessService;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.security.AuthUser;
import com.algorycarservice.security.JwtService;
import com.algorycarservice.user.dto.UserResponse;
import com.algorycarservice.user.model.Role;
import com.algorycarservice.user.model.User;
import com.algorycarservice.user.repository.UserRepository;
import com.algorycarservice.user.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final BusinessService businessService;

    /**
     * Open workshop sign-up exists only to create the first admin; after that admins add staff accounts.
     * OtoViser customers and providers do not count: they sign up on their own at any time.
     */
    public boolean setupRequired() {
        return userRepository.countByRoleIn(Role.WORKSHOP) == 0;
    }

    /** OtoViser customer sign-up. */
    @Transactional
    public AuthResponse registerCustomer(CustomerRegisterRequest request) {
        User user = userRepository.save(User.builder()
                .email(uniqueEmail(request.email()))
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build());
        return authenticated(user, refreshTokenService.startSession(user));
    }

    /** OtoViser business sign-up: the owner account and its listed business, sharing one business UUID. */
    @Transactional
    public AuthResponse registerProvider(ProviderRegisterRequest request) {
        User user = userRepository.save(User.builder()
                .businessUuid(UUID.randomUUID())
                .email(uniqueEmail(request.email()))
                .fullName(request.fullName().trim())
                .phone(request.business().phone().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.PROVIDER)
                .build());
        businessService.create(user, request.business());
        return authenticated(user, refreshTokenService.startSession(user));
    }

    private String uniqueEmail(String email) {
        String normalized = StaffService.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return normalized;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!setupRequired()) {
            throw new ApiException(ErrorCode.REGISTRATION_CLOSED);
        }
        User user = userRepository.save(User.builder()
                .businessUuid(UUID.randomUUID())
                .email(StaffService.normalizeEmail(request.email()))
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .build());
        return authenticated(user, refreshTokenService.startSession(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(StaffService.normalizeEmail(request.email()))
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return authenticated(user, refreshTokenService.startSession(user));
    }

    /** Not transactional on purpose: reuse detection inside {@code rotate} must commit even when refresh fails. */
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        return authenticated(rotation.user(), rotation.next());
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    /** Signs out every device and hands the calling device a fresh session. */
    @Transactional
    public AuthResponse changePassword(AuthUser authUser, ChangePasswordRequest request) {
        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTHENTICATION_REQUIRED));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user.getId());
        return authenticated(user, refreshTokenService.startSession(user));
    }

    private AuthResponse authenticated(User user, RefreshTokenService.IssuedRefreshToken refreshToken) {
        JwtService.IssuedToken token = jwtService.issue(
                new AuthUser(user.getId(), user.getEmail(), user.getRole(), refreshToken.sessionId()));
        return new AuthResponse(token.value(), TOKEN_TYPE, token.expiresAt(), refreshToken.value(), UserResponse.from(user));
    }
}
