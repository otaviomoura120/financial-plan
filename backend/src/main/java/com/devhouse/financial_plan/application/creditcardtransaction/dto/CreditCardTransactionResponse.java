package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CreditCardTransactionResponse(Long id, Integer version, Long creditCardId, Long userId, Long categoryId,
                                             Long subCategoryId, BigDecimal amount, LocalDate purchaseDate, String description,
                                             LocalDate referenceMonth, String installmentGroupId, Integer installmentNumber,
                                             Integer totalInstallments, boolean anticipated, LocalDate originalReferenceMonth,
                                             Instant createdDate, BigDecimal totalAmount) {}
