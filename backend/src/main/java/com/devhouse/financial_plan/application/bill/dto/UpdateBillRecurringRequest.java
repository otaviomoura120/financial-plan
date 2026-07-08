package com.devhouse.financial_plan.application.bill.dto;

import java.math.BigDecimal;

public record UpdateBillRecurringRequest(Integer version, String name, Long categoryId, Long subCategoryId,
                                          BigDecimal defaultAmount) {}
