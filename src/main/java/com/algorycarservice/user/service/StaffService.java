package com.algorycarservice.user.service;

import com.algorycarservice.auth.service.RefreshTokenService;
import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.user.dto.StaffCreateRequest;
import com.algorycarservice.user.dto.StaffUpdateRequest;
import com.algorycarservice.user.dto.UserResponse;
import com.algorycarservice.user.model.User;
import com.algorycarservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public List<UserResponse> list() {
        return userRepository.findAllByOrderByActiveDescFullNameAsc().stream().map(UserResponse::from).toList();
    }

    public UserResponse get(Long id) {
        return UserResponse.from(getUser(id));
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("user"));
    }

    @Transactional
    public UserResponse create(Long adminId, StaffCreateRequest request) {
        User admin = getUser(adminId);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        User user = userRepository.save(User.builder()
                .businessUuid(admin.getBusinessUuid())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(clean(request.phone()))
                .position(clean(request.position()))
                .role(request.role())
                .annualLeaveDays(request.annualLeaveDays())
                .build());
        return UserResponse.from(user);
    }

    /** An admin cannot demote or deactivate themselves, so the workshop never ends up without an admin. */
    @Transactional
    public UserResponse update(Long actingUserId, Long id, StaffUpdateRequest request) {
        User user = getUser(id);
        boolean self = user.getId().equals(actingUserId);
        if (self && (request.role() != user.getRole() || !request.active())) {
            throw new ApiException(ErrorCode.CANNOT_CHANGE_OWN_ACCOUNT);
        }
        user.setFullName(request.fullName().trim());
        user.setPhone(clean(request.phone()));
        user.setPosition(clean(request.position()));
        user.setRole(request.role());
        user.setAnnualLeaveDays(request.annualLeaveDays());
        boolean signOut = user.isActive() && !request.active();
        user.setActive(request.active());
        if (StringUtils.hasText(request.newPassword())) {
            if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            signOut = true;
        }
        if (signOut) {
            refreshTokenService.revokeAll(user.getId());
        }
        return UserResponse.from(user);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
