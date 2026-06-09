package com.devhouse.financial_plan.application.report.dto;

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponse(
        List<TransactionResponse> transactions,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {}
