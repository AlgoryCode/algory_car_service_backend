package com.algorycarservice.workorder.controller;

import com.algorycarservice.workorder.dto.PaymentRequest;
import com.algorycarservice.workorder.dto.StatusRequest;
import com.algorycarservice.workorder.dto.WorkOrderRequest;
import com.algorycarservice.workorder.dto.WorkOrderResponse;
import com.algorycarservice.workorder.model.WorkOrderStatus;
import com.algorycarservice.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /** @param status optional comma separated filter, e.g. {@code OPEN,IN_PROGRESS} */
    @GetMapping
    public List<WorkOrderResponse> list(@RequestParam(required = false) Set<WorkOrderStatus> status) {
        return workOrderService.list(status);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable Long id) {
        return workOrderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse create(@Valid @RequestBody WorkOrderRequest request) {
        return workOrderService.create(request);
    }

    @PutMapping("/{id}")
    public WorkOrderResponse update(@PathVariable Long id, @Valid @RequestBody WorkOrderRequest request) {
        return workOrderService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public WorkOrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return workOrderService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkOrderResponse markPaid(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return workOrderService.markPaid(id, request);
    }

    @DeleteMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkOrderResponse cancelPayment(@PathVariable Long id) {
        return workOrderService.cancelPayment(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        workOrderService.delete(id);
    }
}
