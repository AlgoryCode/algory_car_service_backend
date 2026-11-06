package com.algorycarservice.market.controller;

import com.algorycarservice.market.dto.BookingResponse;
import com.algorycarservice.market.dto.BookingStatusRequest;
import com.algorycarservice.market.dto.BusinessRequest;
import com.algorycarservice.market.dto.BusinessResponse;
import com.algorycarservice.market.model.BookingStatus;
import com.algorycarservice.market.service.BookingService;
import com.algorycarservice.market.service.BusinessService;
import com.algorycarservice.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/** OtoViser business side: the owner's listing and the bookings it receives. */
@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
public class ProviderController {

    private final BusinessService businessService;
    private final BookingService bookingService;

    @GetMapping("/business")
    public BusinessResponse business(@AuthenticationPrincipal AuthUser user) {
        return businessService.mine(user.id());
    }

    @PutMapping("/business")
    public BusinessResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody BusinessRequest request) {
        return businessService.update(user.id(), request);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings(@AuthenticationPrincipal AuthUser user,
                                          @RequestParam(required = false) Set<BookingStatus> status) {
        return bookingService.forBusiness(user.id(), status);
    }

    @PutMapping("/bookings/{id}/status")
    public BookingResponse changeStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                        @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.changeStatus(user.id(), id, request);
    }
}
