package com.devhouse.financial_plan.application.report.dto;

import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.time.LocalDate;

public record ReportFilterRequest(
        Long spaceId,
        LocalDate from,
        LocalDate to,
        Long userId,
        Long bankAccountId,
        Long categoryId,
        Long subCategoryId,
        Long paymentMethodId,
        TransactionType type
) {}
