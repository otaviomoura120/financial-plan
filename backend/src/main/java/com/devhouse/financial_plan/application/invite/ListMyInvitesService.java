package com.devhouse.financial_plan.application.invite;

import com.devhouse.financial_plan.application.invite.dto.MyInviteResponse;
import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.InviteStatus;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMyInvitesService {

    private final SpaceInviteRepository spaceInviteRepository;
    private final UserRepository userRepository;

    public ListMyInvitesService(SpaceInviteRepository spaceInviteRepository, UserRepository userRepository) {
        this.spaceInviteRepository = spaceInviteRepository;
        this.userRepository = userRepository;
    }

    public List<MyInviteResponse> execute(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            return List.of();
        }

        return spaceInviteRepository.findByEmailIgnoreCaseAndStatus(user.getEmail(), InviteStatus.PENDING)
                .stream()
                .filter(invite -> !invite.isExpired())
                .map(this::toResponse)
                .toList();
    }

    private MyInviteResponse toResponse(SpaceInvite invite) {
        return new MyInviteResponse(
                invite.getToken(),
                invite.getSpace().getId(),
                invite.getSpace().getName(),
                invite.getRole().getId(),
                invite.getRole().getName(),
                invite.getExpiresAt()
        );
    }
}
