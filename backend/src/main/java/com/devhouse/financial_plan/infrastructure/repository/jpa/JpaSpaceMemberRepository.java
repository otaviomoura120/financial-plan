package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaSpaceMemberRepository extends JpaRepository<SpaceMemberEntityJpa, Long> {

    Optional<SpaceMemberEntityJpa> findBySpaceIdAndUserId(Long spaceId, Long userId);
    List<SpaceMemberEntityJpa> findBySpaceId(Long spaceId);
    List<SpaceMemberEntityJpa> findByUserId(Long userId);
}
