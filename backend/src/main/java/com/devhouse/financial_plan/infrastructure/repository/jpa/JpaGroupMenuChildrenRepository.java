package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaGroupMenuChildrenRepository extends JpaRepository<GroupMenuChildrenEntityJpa, Long> {

    List<GroupMenuChildrenEntityJpa> findByGroupMenuId(Long groupMenuId);
}
