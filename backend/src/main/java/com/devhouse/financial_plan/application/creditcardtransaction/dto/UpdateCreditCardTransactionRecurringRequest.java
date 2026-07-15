package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCreditCardTransactionRecurringRequest(Integer version, Long categoryId, Long subCategoryId,
                                                           BigDecimal defaultAmount, String description, LocalDate startDate) {}
