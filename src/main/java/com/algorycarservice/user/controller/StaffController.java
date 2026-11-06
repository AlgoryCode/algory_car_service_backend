package com.algorycarservice.user.controller;

import com.algorycarservice.security.AuthUser;
import com.algorycarservice.user.dto.StaffCreateRequest;
import com.algorycarservice.user.dto.StaffUpdateRequest;
import com.algorycarservice.user.dto.UserResponse;
import com.algorycarservice.user.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everyone may read the list (technician pickers, leave calendar); only admins change it. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return staffService.get(user.id());
    }

    @GetMapping("/staff")
    public List<UserResponse> list() {
        return staffService.list();
    }

    @GetMapping("/staff/{id}")
    public UserResponse get(@PathVariable Long id) {
        return staffService.get(id);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody StaffCreateRequest request) {
        return staffService.create(user.id(), request);
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@AuthenticationPrincipal AuthUser user,
                               @PathVariable Long id,
                               @Valid @RequestBody StaffUpdateRequest request) {
        return staffService.update(user.id(), id, request);
    }
}
