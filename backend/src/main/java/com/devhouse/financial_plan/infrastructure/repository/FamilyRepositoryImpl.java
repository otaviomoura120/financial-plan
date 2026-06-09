package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import org.springframework.stereotype.Component;

@Component
public class FamilyRepositoryImpl implements FamilyRepository {

    @Override
    public Family save(Family family) { return null; }

    @Override
    public Family update(Family family) { return null; }

    @Override
    public Family findById(Long id) { return null; }

    @Override
    public void delete(Long id) {}
}
