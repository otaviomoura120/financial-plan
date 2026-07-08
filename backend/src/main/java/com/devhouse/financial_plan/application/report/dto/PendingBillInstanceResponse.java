package com.devhouse.financial_plan.application.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingBillInstanceResponse(Long billInstanceId, Long billRecurringId, String billName, LocalDate referenceMonth,
                                           LocalDate dueDate, BigDecimal amount) {}
