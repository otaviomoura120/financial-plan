package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.BillInstance;

import java.time.LocalDate;
import java.util.List;

public interface BillInstanceRepository {
    BillInstance save(BillInstance billInstance);
    BillInstance update(BillInstance billInstance);
    BillInstance findById(Long id);
    BillInstance findByBillIdAndReferenceMonth(Long billId, LocalDate referenceMonth);
    List<BillInstance> findByBillId(Long billId);
    List<BillInstance> findBySpaceAndPeriod(Long spaceId, LocalDate from, LocalDate to);
}
