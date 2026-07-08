package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaBillRepository extends JpaRepository<BillEntityJpa, Long> {

    List<BillEntityJpa> findBySpaceId(Long spaceId);
}
