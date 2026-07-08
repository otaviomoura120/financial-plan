package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.BillRecurring;

import java.util.List;

public interface BillRecurringRepository {
    BillRecurring save(BillRecurring billRecurring);
    BillRecurring update(BillRecurring billRecurring);
    BillRecurring findById(Long id);
    List<BillRecurring> findBySpaceId(Long spaceId);
    void delete(Long id);
}
