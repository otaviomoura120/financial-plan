package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCreditCardTransactionRequest(Long creditCardId, Long categoryId, Long subCategoryId,
                                                   BigDecimal amount, Boolean credit, LocalDate purchaseDate, String description,
                                                   Integer totalInstallments, LocalDate referenceMonth) {}
