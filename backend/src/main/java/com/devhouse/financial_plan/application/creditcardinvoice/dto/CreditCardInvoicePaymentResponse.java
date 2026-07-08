package com.devhouse.financial_plan.application.creditcardinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardInvoicePaymentResponse(Long id, Long creditCardId, LocalDate referenceMonth, LocalDate dueDate,
                                                BigDecimal paidAmount, LocalDate paidDate, Long paymentTransactionId,
                                                Long bankAccountId) {}
