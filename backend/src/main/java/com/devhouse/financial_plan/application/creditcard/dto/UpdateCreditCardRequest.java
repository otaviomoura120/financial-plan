package com.devhouse.financial_plan.application.creditcard.dto;

import java.math.BigDecimal;

public record UpdateCreditCardRequest(Integer version, String name, BigDecimal limit, Integer closingDay, Integer dueDay, Long bankAccountId) {}
