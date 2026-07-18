package com.devhouse.financial_plan.application.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingCreditCardInvoiceResponse(Long creditCardId, String creditCardName, LocalDate referenceMonth,
                                                 LocalDate dueDate, BigDecimal amount) {}
