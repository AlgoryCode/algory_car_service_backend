package com.algorycarservice.finance.controller;

import com.algorycarservice.finance.dto.FinanceSummaryResponse;
import com.algorycarservice.finance.dto.TransactionRequest;
import com.algorycarservice.finance.dto.TransactionResponse;
import com.algorycarservice.finance.service.FinanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

/** Income and expenses are visible to admins only. */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    public FinanceSummaryResponse summary(@RequestParam @Min(2000) @Max(2100) int year) {
        return financeService.summary(year);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> list(@RequestParam @Min(2000) @Max(2100) int year,
                                          @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return financeService.list(year, month);
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return financeService.get(id);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return financeService.create(request);
    }

    @PutMapping("/transactions/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return financeService.update(id, request);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        financeService.delete(id);
    }
}
