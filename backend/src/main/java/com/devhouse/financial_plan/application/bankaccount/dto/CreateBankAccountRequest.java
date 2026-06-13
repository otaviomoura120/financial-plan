package com.devhouse.financial_plan.application.bankaccount.dto;

import java.math.BigDecimal;

public record CreateBankAccountRequest(Long spaceId, String name, String bankName, BigDecimal initialBalance) {}
