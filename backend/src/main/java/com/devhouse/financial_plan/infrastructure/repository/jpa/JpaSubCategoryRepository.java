package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSubCategoryRepository extends JpaRepository<SubCategoryEntityJpa, Long> {

    List<SubCategoryEntityJpa> findByCategoryIdOrderByNameAsc(Long categoryId);
    boolean existsByCategoryId(Long categoryId);
}
