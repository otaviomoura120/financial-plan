package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Transaction update(Transaction transaction);
    Transaction findById(Long id);
    List<Transaction> findByFilter(Long spaceId, Long userId, Long bankAccountId, Long categoryId, Long subCategoryId, Long paymentMethodId, TransactionType type, LocalDate from, LocalDate to);
    void delete(Long id);
}
