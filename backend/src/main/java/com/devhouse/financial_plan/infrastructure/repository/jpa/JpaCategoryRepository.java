package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntityJpa, Long> {

    List<CategoryEntityJpa> findBySpaceId(Long spaceId);
}
