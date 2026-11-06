package com.algorycarservice.dashboard.controller;

import com.algorycarservice.dashboard.dto.DashboardResponse;
import com.algorycarservice.dashboard.service.DashboardService;
import com.algorycarservice.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal AuthUser user) {
        return dashboardService.dashboard(user);
    }
}
