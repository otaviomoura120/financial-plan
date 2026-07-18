package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCreditCardTransactionRequest(Integer version, Long categoryId, Long subCategoryId,
                                                   BigDecimal amount, LocalDate purchaseDate, String description) {}
