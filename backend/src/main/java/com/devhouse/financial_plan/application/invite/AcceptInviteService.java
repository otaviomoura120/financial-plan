package com.devhouse.financial_plan.application.invite;

import com.devhouse.financial_plan.application.invite.dto.AcceptInviteResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.InviteStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AcceptInviteService {

    private final SpaceInviteRepository spaceInviteRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final UserRepository userRepository;

    public AcceptInviteService(SpaceInviteRepository spaceInviteRepository,
                               SpaceMemberRepository spaceMemberRepository,
                               UserRepository userRepository) {
        this.spaceInviteRepository = spaceInviteRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.userRepository = userRepository;
    }

    public AcceptInviteResponse execute(String token, String auth0Sub) {
        SpaceInvite invite = spaceInviteRepository.findByToken(token)
                .orElseThrow(() -> new DomainException("Invite not found"));

        if (invite.getStatus() == InviteStatus.CANCELLED) {
            throw new DomainException("invite_cancelled");
        }
        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new DomainException("invite_already_accepted");
        }
        if (invite.isExpired()) {
            throw new DomainException("invite_expired");
        }

        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("complete_onboarding");
        }

        if (!user.getEmail().equalsIgnoreCase(invite.getEmail())) {
            throw new DomainException("invite_email_mismatch");
        }

        SpaceMember existing = spaceMemberRepository.findBySpaceIdAndUserId(
                invite.getSpace().getId(), user.getId());
        if (existing != null) {
            throw new DomainException("already_member");
        }

        Space space = invite.getSpace();
        Role role = invite.getRole();
        SpaceMember member = new SpaceMember(null, space, user, role, Instant.now());
        member.validate();
        spaceMemberRepository.save(member);

        invite.accept();
        spaceInviteRepository.update(invite);

        return new AcceptInviteResponse(
                space.getId(),
                space.getName(),
                role.getId(),
                role.getName(),
                user.getId()
        );
    }
}
