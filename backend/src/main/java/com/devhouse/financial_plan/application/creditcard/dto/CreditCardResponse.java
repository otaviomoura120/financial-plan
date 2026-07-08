package com.devhouse.financial_plan.application.creditcard.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditCardResponse(Long id, Integer version, Long spaceId, String name, BigDecimal limit, Integer closingDay, Integer dueDay, boolean active, Instant createdDate) {}
