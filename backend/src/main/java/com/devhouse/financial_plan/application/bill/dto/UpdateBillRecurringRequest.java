package com.devhouse.financial_plan.application.bill.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBillRecurringRequest(Integer version, String name, Long categoryId, Long subCategoryId,
                                          BigDecimal defaultAmount, LocalDate startDate) {}
