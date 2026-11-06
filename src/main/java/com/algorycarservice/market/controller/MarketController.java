package com.algorycarservice.market.controller;

import com.algorycarservice.appointment.dto.SlotsResponse;
import com.algorycarservice.market.dto.BookingRequest;
import com.algorycarservice.market.dto.BookingResponse;
import com.algorycarservice.market.dto.BusinessResponse;
import com.algorycarservice.market.model.ServiceCategory;
import com.algorycarservice.market.service.BookingService;
import com.algorycarservice.market.service.BusinessService;
import com.algorycarservice.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** OtoViser customer side: find a business and book it. Browsing is open to providers too (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final BusinessService businessService;
    private final BookingService bookingService;

    @GetMapping("/businesses")
    public List<BusinessResponse> search(@RequestParam(required = false) ServiceCategory category,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) String q) {
        return businessService.search(category, city, q);
    }

    @GetMapping("/businesses/{id}")
    public BusinessResponse get(@PathVariable Long id) {
        return businessService.get(id);
    }

    @GetMapping("/businesses/{id}/slots")
    public SlotsResponse slots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return businessService.slots(id, date);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody BookingRequest request) {
        return bookingService.create(user.id(), request);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthUser user) {
        return bookingService.mine(user.id());
    }

    /** Body is optional: {@code {"note": "..."}}. */
    @PutMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body) {
        return bookingService.cancel(user.id(), id, body != null ? body.get("note") : null);
    }
}
