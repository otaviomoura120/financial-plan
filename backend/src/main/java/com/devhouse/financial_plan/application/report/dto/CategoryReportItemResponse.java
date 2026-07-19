package com.devhouse.financial_plan.application.report.dto;

import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CategoryReportItemResponse(
        Long id,
        CategoryReportItemSource source,
        TransactionType type,
        LocalDate date,
        String description,
        BigDecimal amount,
        Long userId,
        Long bankAccountId,
        Long creditCardId,
        String creditCardName,
        Integer installmentNumber,
        Integer totalInstallments,
        BigDecimal totalAmount,
        LocalDate referenceMonth,
        LocalDate dueDate
) {}
