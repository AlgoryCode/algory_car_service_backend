package com.algorycarservice.auth.controller;

import com.algorycarservice.auth.dto.AuthResponse;
import com.algorycarservice.auth.dto.ChangePasswordRequest;
import com.algorycarservice.auth.dto.LoginRequest;
import com.algorycarservice.auth.dto.RefreshTokenRequest;
import com.algorycarservice.auth.dto.RegisterRequest;
import com.algorycarservice.auth.dto.SetupStatusResponse;
import com.algorycarservice.auth.service.AuthService;
import com.algorycarservice.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/setup-status")
    public SetupStatusResponse setupStatus() {
        return new SetupStatusResponse(authService.setupRequired());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @PutMapping("/password")
    public AuthResponse changePassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(user, request);
    }
}
