package com.devhouse.financial_plan.application.creditcardinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardInvoiceResponse(Long creditCardId, String creditCardName, LocalDate referenceMonth,
                                         LocalDate closingDate, LocalDate dueDate, BigDecimal totalAmount, boolean paid,
                                         LocalDate paidDate, BigDecimal paidAmount, Long paymentTransactionId) {}
