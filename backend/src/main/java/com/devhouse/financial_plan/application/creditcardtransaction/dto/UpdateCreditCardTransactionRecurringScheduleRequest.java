package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.time.LocalDate;

public record UpdateCreditCardTransactionRecurringScheduleRequest(Integer version, LocalDate startDate) {}
