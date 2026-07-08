package com.devhouse.financial_plan.application.bill.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBillRequest(Long spaceId, String name, Long categoryId, BigDecimal defaultAmount,
                                 LocalDate startDate, boolean recurring) {}
