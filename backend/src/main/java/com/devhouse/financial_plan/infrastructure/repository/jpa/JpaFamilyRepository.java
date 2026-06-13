package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFamilyRepository extends JpaRepository<FamilyEntityJpa, Long> {
}
