package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;

public record UpdateCreditCardTransactionRecurringRequest(Integer version, Long categoryId, Long subCategoryId,
                                                           BigDecimal defaultAmount, String description) {}
