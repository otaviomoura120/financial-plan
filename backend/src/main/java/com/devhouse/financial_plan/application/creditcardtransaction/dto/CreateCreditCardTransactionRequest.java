package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCreditCardTransactionRequest(Long creditCardId, Long userId, Long categoryId, Long subCategoryId,
                                                   BigDecimal amount, LocalDate purchaseDate, String description,
                                                   Integer totalInstallments) {}
