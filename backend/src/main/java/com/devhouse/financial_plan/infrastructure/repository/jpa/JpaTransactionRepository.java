package com.devhouse.financial_plan.infrastructure.repository.jpa;

import com.devhouse.financial_plan.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntityJpa, Long>, JpaSpecificationExecutor<TransactionEntityJpa> {

    /**
     * Net effect the given accounts suffered from every transaction dated on or after {@code from},
     * as a single aggregate: credits minus debits. A transfer credits its destination and debits its
     * origin, so it only nets to zero when both accounts are inside {@code bankAccountIds}.
     * Returns null when no row matches.
     */
    @Query("""
            SELECT SUM(CASE WHEN t.type = com.devhouse.financial_plan.domain.enums.TransactionType.INCOME
                                  AND t.bankAccount.id IN :bankAccountIds THEN t.amount ELSE 0 END)
                 - SUM(CASE WHEN t.type = com.devhouse.financial_plan.domain.enums.TransactionType.EXPENSE
                                  AND t.bankAccount.id IN :bankAccountIds THEN t.amount ELSE 0 END)
                 + SUM(CASE WHEN t.type = com.devhouse.financial_plan.domain.enums.TransactionType.TRANSFER
                                  AND t.destinationBankAccount.id IN :bankAccountIds THEN t.amount ELSE 0 END)
                 - SUM(CASE WHEN t.type = com.devhouse.financial_plan.domain.enums.TransactionType.TRANSFER
                                  AND t.bankAccount.id IN :bankAccountIds THEN t.amount ELSE 0 END)
            FROM TransactionEntityJpa t
            WHERE t.transactionDate >= :from
              AND (t.bankAccount.id IN :bankAccountIds OR t.destinationBankAccount.id IN :bankAccountIds)
            """)
    BigDecimal sumBalanceEffectFrom(@Param("bankAccountIds") List<Long> bankAccountIds, @Param("from") LocalDate from);

    boolean existsByBankAccountIdOrDestinationBankAccountId(Long bankAccountId, Long destinationBankAccountId);
    boolean existsByCategoryId(Long categoryId);
    boolean existsBySubCategoryId(Long subCategoryId);
    long countByCategoryIdAndType(Long categoryId, TransactionType type);
}
