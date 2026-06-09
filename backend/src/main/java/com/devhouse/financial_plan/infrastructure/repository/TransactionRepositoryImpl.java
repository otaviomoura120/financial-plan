package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TransactionRepositoryImpl implements TransactionRepository {

    @Override
    public Transaction save(Transaction transaction) { return null; }

    @Override
    public Transaction update(Transaction transaction) { return null; }

    @Override
    public Transaction findById(Long id) { return null; }

    @Override
    public List<Transaction> findByFilter(Long userId, Long bankAccountId, Long categoryId, Long subCategoryId, Long paymentMethodId, TransactionType type, LocalDate from, LocalDate to) { return List.of(); }

    @Override
    public void delete(Long id) {}
}
