package com.devhouse.financial_plan.infrastructure.repository.jpa;

import com.devhouse.financial_plan.domain.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaSpaceInviteRepository extends JpaRepository<SpaceInviteEntityJpa, Long> {

    Optional<SpaceInviteEntityJpa> findByToken(String token);
    List<SpaceInviteEntityJpa> findBySpaceId(Long spaceId);
    Optional<SpaceInviteEntityJpa> findBySpaceIdAndEmail(Long spaceId, String email);
    List<SpaceInviteEntityJpa> findByEmailIgnoreCaseAndStatus(String email, InviteStatus status);
}
