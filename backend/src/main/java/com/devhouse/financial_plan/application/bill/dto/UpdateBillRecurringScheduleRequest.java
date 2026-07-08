package com.devhouse.financial_plan.application.bill.dto;

import java.time.LocalDate;

public record UpdateBillRecurringScheduleRequest(Integer version, LocalDate startDate) {}
