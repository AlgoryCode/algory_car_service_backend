package com.algorycarservice.finance.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The category decides whether money comes in or goes out, so clients never send the type separately. */
@Getter
@RequiredArgsConstructor
public enum TransactionCategory {
    SERVICE(TransactionType.INCOME),
    PARTS_SALE(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME),
    PARTS_PURCHASE(TransactionType.EXPENSE),
    SALARY(TransactionType.EXPENSE),
    RENT(TransactionType.EXPENSE),
    UTILITIES(TransactionType.EXPENSE),
    TAX(TransactionType.EXPENSE),
    EQUIPMENT(TransactionType.EXPENSE),
    OTHER_EXPENSE(TransactionType.EXPENSE);

    private final TransactionType type;
}
