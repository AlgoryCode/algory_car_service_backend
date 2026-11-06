package com.algorycarservice.finance.dto;

import com.algorycarservice.finance.model.TransactionCategory;

import java.math.BigDecimal;
import java.util.List;

/** Year at a glance: one row per month (always 12) and totals per category, largest first. */
public record FinanceSummaryResponse(
        int year,
        List<MonthTotal> months,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net,
        List<CategoryTotal> incomeByCategory,
        List<CategoryTotal> expenseByCategory
) {

    public record MonthTotal(int month, BigDecimal income, BigDecimal expense, BigDecimal net) {
    }

    public record CategoryTotal(TransactionCategory category, BigDecimal amount) {
    }
}
