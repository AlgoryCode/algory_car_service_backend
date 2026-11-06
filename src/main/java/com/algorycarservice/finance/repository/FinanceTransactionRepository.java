package com.algorycarservice.finance.repository;

import com.algorycarservice.finance.model.FinanceTransaction;
import com.algorycarservice.finance.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    List<FinanceTransaction> findAllByTransactionDateBetweenOrderByTransactionDateDescIdDesc(LocalDate from, LocalDate to);

    Optional<FinanceTransaction> findByWorkOrderId(Long workOrderId);

    @Query("""
            select coalesce(sum(t.amount), 0) from FinanceTransaction t
            where t.type = :type and t.transactionDate between :from and :to
            """)
    BigDecimal sumBetween(@Param("type") TransactionType type, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
