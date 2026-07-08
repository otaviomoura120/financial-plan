package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Bill;

import java.time.LocalDate;
import java.util.List;

public interface BillRepository {
    Bill save(Bill bill);
    Bill update(Bill bill);
    Bill findById(Long id);
    Bill findByBillRecurringIdAndReferenceMonth(Long billRecurringId, LocalDate referenceMonth);
    List<Bill> findByBillRecurringId(Long billRecurringId);
    List<Bill> findBySpaceAndPeriod(Long spaceId, LocalDate from, LocalDate to);
    List<Bill> findBySpaceAndPeriod(Long spaceId, LocalDate from, LocalDate to, Long categoryId, Long subCategoryId);
}
