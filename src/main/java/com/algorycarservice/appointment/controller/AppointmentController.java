package com.algorycarservice.appointment.controller;

import com.algorycarservice.appointment.dto.AppointmentRequest;
import com.algorycarservice.appointment.dto.AppointmentResponse;
import com.algorycarservice.appointment.dto.AppointmentStatusRequest;
import com.algorycarservice.appointment.dto.SlotsResponse;
import com.algorycarservice.appointment.model.AppointmentStatus;
import com.algorycarservice.appointment.service.AppointmentService;
import com.algorycarservice.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.Set;

/** Booked visits, mainly vehicle inspections. Everyone books and handles them; admins confirm and delete. */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /** Defaults to the coming 30 days; {@code status} may repeat. */
    @GetMapping
    public List<AppointmentResponse> list(@RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) Set<AppointmentStatus> status) {
        return appointmentService.list(from, to, status);
    }

    @GetMapping("/slots")
    public SlotsResponse slots(@RequestParam LocalDate date) {
        return appointmentService.slots(date);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.create(user, request);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public AppointmentResponse changeStatus(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.changeStatus(user, id, request);
    }

    /** The customer arrived: opens a work order for the vehicle and completes the appointment. */
    @PostMapping("/{id}/work-order")
    public AppointmentResponse startWorkOrder(@PathVariable Long id) {
        return appointmentService.startWorkOrder(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        appointmentService.delete(id);
    }
}
