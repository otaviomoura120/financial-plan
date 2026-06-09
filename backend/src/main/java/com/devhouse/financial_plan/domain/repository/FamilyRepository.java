package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Family;

public interface FamilyRepository {
    Family save(Family family);
    Family update(Family family);
    Family findById(Long id);
    void delete(Long id);
}
