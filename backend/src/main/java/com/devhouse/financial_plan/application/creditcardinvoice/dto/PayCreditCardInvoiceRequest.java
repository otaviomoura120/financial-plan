package com.devhouse.financial_plan.application.creditcardinvoice.dto;

import java.time.LocalDate;

public record PayCreditCardInvoiceRequest(Long bankAccountId, Long categoryId, Long subCategoryId, Long paymentMethodId, LocalDate paidDate) {}
