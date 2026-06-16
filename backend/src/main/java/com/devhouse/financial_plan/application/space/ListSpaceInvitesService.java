package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceInviteResponse;
import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSpaceInvitesService {

    private final SpaceInviteRepository spaceInviteRepository;

    public ListSpaceInvitesService(SpaceInviteRepository spaceInviteRepository) {
        this.spaceInviteRepository = spaceInviteRepository;
    }

    public List<SpaceInviteResponse> execute(Long spaceId) {
        return spaceInviteRepository.findBySpaceId(spaceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SpaceInviteResponse toResponse(SpaceInvite invite) {
        return new SpaceInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole().getId(),
                invite.getRole().getName(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt()
        );
    }
}
