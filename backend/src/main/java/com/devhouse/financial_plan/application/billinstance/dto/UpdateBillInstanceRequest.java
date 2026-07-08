package com.devhouse.financial_plan.application.billinstance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBillInstanceRequest(Integer version, String name, Long categoryId, Long subCategoryId, BigDecimal amount,
                                         LocalDate dueDate) {}
