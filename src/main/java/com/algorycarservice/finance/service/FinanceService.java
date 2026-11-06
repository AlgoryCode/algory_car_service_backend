package com.algorycarservice.finance.service;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import com.algorycarservice.finance.dto.FinanceSummaryResponse;
import com.algorycarservice.finance.dto.FinanceSummaryResponse.CategoryTotal;
import com.algorycarservice.finance.dto.FinanceSummaryResponse.MonthTotal;
import com.algorycarservice.finance.dto.TransactionRequest;
import com.algorycarservice.finance.dto.TransactionResponse;
import com.algorycarservice.finance.model.FinanceTransaction;
import com.algorycarservice.finance.model.TransactionCategory;
import com.algorycarservice.finance.model.TransactionType;
import com.algorycarservice.finance.repository.FinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

    private final FinanceTransactionRepository repository;

    /** @param month 1–12, or {@code null} for the whole year */
    public List<TransactionResponse> list(int year, Integer month) {
        LocalDate from = month == null ? LocalDate.of(year, 1, 1) : YearMonth.of(year, month).atDay(1);
        LocalDate to = month == null ? LocalDate.of(year, 12, 31) : YearMonth.of(year, month).atEndOfMonth();
        return repository.findAllByTransactionDateBetweenOrderByTransactionDateDescIdDesc(from, to).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    public TransactionResponse get(Long id) {
        return TransactionResponse.from(getTransaction(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        FinanceTransaction transaction = FinanceTransaction.builder().build();
        apply(transaction, request);
        return TransactionResponse.from(repository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        FinanceTransaction transaction = getEditable(id);
        apply(transaction, request);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEditable(id));
    }

    public FinanceSummaryResponse summary(int year) {
        List<FinanceTransaction> transactions = repository.findAllByTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        BigDecimal[] income = zeros();
        BigDecimal[] expense = zeros();
        Map<TransactionCategory, BigDecimal> byCategory = new EnumMap<>(TransactionCategory.class);
        for (FinanceTransaction transaction : transactions) {
            int index = transaction.getTransactionDate().getMonthValue() - 1;
            BigDecimal[] bucket = transaction.getType() == TransactionType.INCOME ? income : expense;
            bucket[index] = bucket[index].add(transaction.getAmount());
            byCategory.merge(transaction.getCategory(), transaction.getAmount(), BigDecimal::add);
        }

        List<MonthTotal> months = IntStream.range(0, 12)
                .mapToObj(i -> new MonthTotal(i + 1, income[i], expense[i], income[i].subtract(expense[i])))
                .toList();
        BigDecimal totalIncome = sum(income);
        BigDecimal totalExpense = sum(expense);
        return new FinanceSummaryResponse(year, months, totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                categoryTotals(byCategory, TransactionType.INCOME), categoryTotals(byCategory, TransactionType.EXPENSE));
    }

    public BigDecimal total(TransactionType type, LocalDate from, LocalDate to) {
        return repository.sumBetween(type, from, to);
    }

    /** Books the income for a collected work order. */
    @Transactional
    public void recordWorkOrderPayment(Long workOrderId, BigDecimal amount, LocalDate date, String description) {
        repository.save(FinanceTransaction.builder()
                .type(TransactionType.INCOME)
                .category(TransactionCategory.SERVICE)
                .amount(amount)
                .transactionDate(date)
                .description(description)
                .workOrderId(workOrderId)
                .build());
    }

    /** Keeps the booked income equal to the work order total when its lines change after payment. */
    @Transactional
    public void updateWorkOrderPayment(Long workOrderId, BigDecimal amount) {
        repository.findByWorkOrderId(workOrderId).ifPresent(transaction -> transaction.setAmount(amount));
    }

    @Transactional
    public void removeWorkOrderPayment(Long workOrderId) {
        repository.findByWorkOrderId(workOrderId).ifPresent(repository::delete);
    }

    private FinanceTransaction getTransaction(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("transaction"));
    }

    private FinanceTransaction getEditable(Long id) {
        FinanceTransaction transaction = getTransaction(id);
        if (transaction.getWorkOrderId() != null) {
            throw new ApiException(ErrorCode.LINKED_TRANSACTION);
        }
        return transaction;
    }

    private static void apply(FinanceTransaction transaction, TransactionRequest request) {
        transaction.setCategory(request.category());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
    }

    private static List<CategoryTotal> categoryTotals(Map<TransactionCategory, BigDecimal> byCategory, TransactionType type) {
        return byCategory.entrySet().stream()
                .filter(entry -> entry.getKey().getType() == type)
                .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategoryTotal::amount).reversed())
                .toList();
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] values = new BigDecimal[12];
        Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private static BigDecimal sum(BigDecimal[] values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total;
    }
}
