package com.devhouse.financial_plan.application.bankaccount.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BankAccountResponse(Long id, Integer version, Long spaceId, String name, String bankName, BigDecimal balance, boolean active, Instant createdDate) {}
