package com.devhouse.financial_plan.application.billinstance.dto;

import java.math.BigDecimal;

public record UpdateBillInstanceAmountRequest(Integer version, BigDecimal amount) {}
