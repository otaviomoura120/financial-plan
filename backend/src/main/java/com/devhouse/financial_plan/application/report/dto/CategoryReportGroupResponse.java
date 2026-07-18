package com.devhouse.financial_plan.application.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryReportGroupResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal total,
        BigDecimal incomePercentage,
        BigDecimal expensePercentage,
        List<CategoryReportSubGroupResponse> subGroups
) {}
