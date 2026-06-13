package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaEndpointPermissionRepository extends JpaRepository<EndpointPermissionEntityJpa, Long> {

    List<EndpointPermissionEntityJpa> findByTypeOrderBySequenceAsc(String type);

    List<EndpointPermissionEntityJpa> findAllByOrderBySequenceAsc();
}
