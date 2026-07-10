package com.devhouse.financial_plan.application.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryReportResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategoryReportGroupResponse> groups
) {}
