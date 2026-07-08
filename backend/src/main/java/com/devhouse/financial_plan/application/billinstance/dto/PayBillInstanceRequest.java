package com.devhouse.financial_plan.application.billinstance.dto;

import java.time.LocalDate;

public record PayBillInstanceRequest(Long bankAccountId, Long categoryId, Long paymentMethodId, LocalDate paidDate) {}
