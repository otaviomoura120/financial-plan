package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Bill;

import java.util.List;

public interface BillRepository {
    Bill save(Bill bill);
    Bill update(Bill bill);
    Bill findById(Long id);
    List<Bill> findBySpaceId(Long spaceId);
    void delete(Long id);
}
