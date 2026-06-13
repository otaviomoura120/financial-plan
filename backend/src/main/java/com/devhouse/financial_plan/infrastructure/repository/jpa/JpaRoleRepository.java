package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRoleRepository extends JpaRepository<RoleEntityJpa, Long> {

    List<RoleEntityJpa> findBySpaceId(Long spaceId);
}
