package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCreditCardTransactionRecurringRequest(Long creditCardId, Long userId, Long categoryId, Long subCategoryId,
                                                           String description, BigDecimal defaultAmount, LocalDate startDate) {}
