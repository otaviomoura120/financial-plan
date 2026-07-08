package com.devhouse.financial_plan.application.billinstance.dto;

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillInstanceResponse(Long id, Integer version, Long billId, String billName, LocalDate referenceMonth,
                                    LocalDate dueDate, BigDecimal amount, BillInstanceStatus status, LocalDate paidDate,
                                    Long paymentTransactionId, Long bankAccountId, Instant createdDate) {}
