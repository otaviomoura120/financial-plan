package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.enums.InviteStatus;

import java.util.List;
import java.util.Optional;

public interface SpaceInviteRepository {

    SpaceInvite save(SpaceInvite invite);
    SpaceInvite update(SpaceInvite invite);
    Optional<SpaceInvite> findByToken(String token);
    List<SpaceInvite> findBySpaceId(Long spaceId);
    Optional<SpaceInvite> findBySpaceIdAndEmail(Long spaceId, String email);
    List<SpaceInvite> findByEmailIgnoreCaseAndStatus(String email, InviteStatus status);
    void delete(Long id);
}
