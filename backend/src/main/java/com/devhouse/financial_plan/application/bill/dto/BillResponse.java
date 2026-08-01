package com.devhouse.financial_plan.application.bill.dto;

import com.devhouse.financial_plan.domain.BillRecurring;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BillResponse(Long id, Integer version, Long spaceId, String name, Long categoryId, Long subCategoryId,
                            BigDecimal defaultAmount, LocalDate startDate, LocalDate endDate, Integer installments,
                            boolean active, Instant createdDate) {

    public static BillResponse from(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.getEndDate(),
                billRecurring.getInstallments(), billRecurring.isActive(), billRecurring.getCreatedDate());
    }
}
