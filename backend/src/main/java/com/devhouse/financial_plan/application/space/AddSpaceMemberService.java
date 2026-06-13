package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.AddSpaceMemberRequest;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AddSpaceMemberService {

    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    public AddSpaceMemberService(SpaceRepository spaceRepository, UserRepository userRepository,
                                 RoleRepository roleRepository, SpaceMemberRepository spaceMemberRepository) {
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.spaceMemberRepository = spaceMemberRepository;
    }

    public void execute(Long spaceId, Long userId, AddSpaceMemberRequest request) {
        Space space = spaceRepository.findById(spaceId);
        User user = userRepository.findById(userId);
        Role role = roleRepository.findById(request.roleId());
        validateNotAlreadyMember(spaceId, userId);
        SpaceMember member = new SpaceMember(null, space, user, role, Instant.now());
        member.validate();
        spaceMemberRepository.save(member);
    }

    private void validateNotAlreadyMember(Long spaceId, Long userId) {
        SpaceMember existing = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId);
        if (existing != null) {
            throw new DomainException("User is already a member of this space");
        }
    }
}
