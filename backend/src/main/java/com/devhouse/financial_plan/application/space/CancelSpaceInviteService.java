package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import org.springframework.stereotype.Service;

@Service
public class CancelSpaceInviteService {

    private final SpaceInviteRepository spaceInviteRepository;

    public CancelSpaceInviteService(SpaceInviteRepository spaceInviteRepository) {
        this.spaceInviteRepository = spaceInviteRepository;
    }

    public void execute(Long spaceId, Long inviteId) {
        SpaceInvite invite = spaceInviteRepository.findBySpaceId(spaceId)
                .stream()
                .filter(i -> i.getId().equals(inviteId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Invite not found for this space"));

        invite.cancel();
        spaceInviteRepository.update(invite);
    }
}
