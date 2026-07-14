package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CreditCardTransactionRecurringResponse(Long id, Integer version, Long creditCardId, Long userId,
                                                      Long categoryId, Long subCategoryId, String description,
                                                      BigDecimal defaultAmount, LocalDate startDate, boolean active,
                                                      Instant createdDate) {}
