package com.devhouse.financial_plan.application.bankaccount.dto;

import java.math.BigDecimal;

public record CreateBankAccountRequest(Long userId, String name, String bankName, BigDecimal initialBalance) {}
