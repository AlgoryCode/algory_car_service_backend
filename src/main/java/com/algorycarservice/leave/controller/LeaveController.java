package com.algorycarservice.leave.controller;

import com.algorycarservice.leave.dto.LeaveBalanceResponse;
import com.algorycarservice.leave.dto.LeaveRequestDto;
import com.algorycarservice.leave.dto.LeaveResponse;
import com.algorycarservice.leave.dto.ReviewRequest;
import com.algorycarservice.leave.model.LeaveStatus;
import com.algorycarservice.leave.service.LeaveService;
import com.algorycarservice.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/mine")
    public List<LeaveResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return leaveService.mine(user.id());
    }

    /** Own balance; admins may pass {@code userId} to see someone else's. */
    @GetMapping("/balance")
    public LeaveBalanceResponse balance(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam @Min(2000) @Max(2100) int year,
                                        @RequestParam(required = false) Long userId) {
        Long target = user.isAdmin() && userId != null ? userId : user.id();
        return leaveService.balance(target, year);
    }

    @GetMapping("/calendar")
    public List<LeaveResponse> calendar(@RequestParam @Min(2000) @Max(2100) int year,
                                        @RequestParam @Min(1) @Max(12) int month) {
        return leaveService.calendar(year, month);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<LeaveResponse> list(@RequestParam(required = false) Set<LeaveStatus> status) {
        return leaveService.byStatus(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LeaveRequestDto request) {
        return leaveService.create(user, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public LeaveResponse approve(@AuthenticationPrincipal AuthUser user,
                                 @PathVariable Long id,
                                 @Valid @RequestBody(required = false) ReviewRequest request) {
        return leaveService.approve(user.id(), id, request == null ? null : request.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public LeaveResponse reject(@AuthenticationPrincipal AuthUser user,
                                @PathVariable Long id,
                                @Valid @RequestBody(required = false) ReviewRequest request) {
        return leaveService.reject(user.id(), id, request == null ? null : request.note());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        leaveService.delete(user, id);
    }
}
