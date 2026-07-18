package com.devhouse.financial_plan.application.transaction.dto;

import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        TransactionType type,
        Long userId,
        Long bankAccountId,
        Long destinationBankAccountId,
        Long categoryId,
        Long subCategoryId,
        BigDecimal amount,
        LocalDate transactionDate,
        String description
) {}
