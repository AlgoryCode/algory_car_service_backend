package com.algorycarservice.finance.dto;

import com.algorycarservice.finance.model.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "{validation.category.required}")
        TransactionCategory category,

        @NotNull(message = "{validation.amount.required}")
        @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
        @Digits(integer = 10, fraction = 2, message = "{validation.amount.digits}")
        BigDecimal amount,

        @NotNull(message = "{validation.date.required}")
        LocalDate transactionDate,

        @Size(max = 200, message = "{validation.note.size}")
        String description
) {
}
