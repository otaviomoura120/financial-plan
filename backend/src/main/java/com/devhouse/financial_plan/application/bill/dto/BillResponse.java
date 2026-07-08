package com.devhouse.financial_plan.application.bill.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillResponse(Long id, Integer version, Long spaceId, String name, Long categoryId,
                            BigDecimal defaultAmount, LocalDate startDate, boolean recurring, boolean active,
                            Instant createdDate) {}
