package com.devhouse.financial_plan.application.report.dto;

import com.devhouse.financial_plan.domain.enums.TransactionType;

import java.time.LocalDate;

public record CategoryReportFilterRequest(
        Long spaceId,
        LocalDate from,
        LocalDate to,
        Long userId,
        Long bankAccountId,
        Long categoryId,
        Long subCategoryId,
        Long paymentMethodId,
        TransactionType type,
        Long creditCardId
) {
    public static final long CREDIT_CARD_PAYMENT_METHOD = -1L;

    public boolean isCreditCardPaymentMethod() {
        return paymentMethodId != null && paymentMethodId == CREDIT_CARD_PAYMENT_METHOD;
    }
}
