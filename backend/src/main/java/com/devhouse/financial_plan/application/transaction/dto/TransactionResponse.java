package com.devhouse.financial_plan.application.transaction.dto;

import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Integer version,
        TransactionType type,
        Long userId,
        Long bankAccountId,
        Long destinationBankAccountId,
        Long categoryId,
        Long subCategoryId,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        Instant createdDate,
        TransactionSourceType sourceType,
        Long sourceId,
        LocalDate creditCardInvoiceReferenceMonth
) {}
