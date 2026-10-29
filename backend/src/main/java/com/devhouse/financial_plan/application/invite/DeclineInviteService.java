package com.devhouse.financial_plan.application.invite;

import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.exception.NotFoundException;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DeclineInviteService {

    private final SpaceInviteRepository spaceInviteRepository;
    private final UserRepository userRepository;

    public DeclineInviteService(SpaceInviteRepository spaceInviteRepository, UserRepository userRepository) {
        this.spaceInviteRepository = spaceInviteRepository;
        this.userRepository = userRepository;
    }

    public void execute(String token, String auth0Sub) {
        SpaceInvite invite = spaceInviteRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        invite.requireAnswerable();

        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("complete_onboarding");
        }

        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new DomainException("invite_email_mismatch");
        }

        invite.decline();
        spaceInviteRepository.update(invite);
    }
}
