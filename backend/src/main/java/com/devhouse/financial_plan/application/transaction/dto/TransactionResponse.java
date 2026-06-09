package com.devhouse.financial_plan.application.transaction.dto;

import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        TransactionType type,
        Long userId,
        Long bankAccountId,
        Long categoryId,
        Long subCategoryId,
        Long paymentMethodId,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        Instant createdDate
) {}
