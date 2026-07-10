package com.devhouse.financial_plan.application.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryReportSubGroupResponse(
        Long subCategoryId,
        String subCategoryName,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal total,
        List<CategoryReportItemResponse> items
) {}
