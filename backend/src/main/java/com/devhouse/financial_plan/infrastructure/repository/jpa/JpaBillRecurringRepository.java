package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaBillRecurringRepository extends JpaRepository<BillRecurringEntityJpa, Long> {

    List<BillRecurringEntityJpa> findBySpaceId(Long spaceId);
}
