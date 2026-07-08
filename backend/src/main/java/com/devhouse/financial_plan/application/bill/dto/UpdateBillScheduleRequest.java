package com.devhouse.financial_plan.application.bill.dto;

import java.time.LocalDate;

public record UpdateBillScheduleRequest(Integer version, boolean recurring, LocalDate startDate) {}
