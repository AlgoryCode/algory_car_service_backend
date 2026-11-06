package com.algorycarservice.finance.dto;

import com.algorycarservice.finance.model.FinanceTransaction;
import com.algorycarservice.finance.model.TransactionCategory;
import com.algorycarservice.finance.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        TransactionType type,
        TransactionCategory category,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        Long workOrderId
) {

    public static TransactionResponse from(FinanceTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getCategory(),
                transaction.getAmount(), transaction.getTransactionDate(), transaction.getDescription(),
                transaction.getWorkOrderId());
    }
}
