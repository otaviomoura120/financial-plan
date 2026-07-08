package com.devhouse.financial_plan.application.bill.dto;

import java.math.BigDecimal;

public record UpdateBillRequest(Integer version, String name, Long categoryId, BigDecimal defaultAmount) {}
