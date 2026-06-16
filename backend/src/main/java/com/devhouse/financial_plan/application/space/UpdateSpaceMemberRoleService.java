package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceMemberResponse;
import com.devhouse.financial_plan.application.space.dto.UpdateSpaceMemberRequest;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateSpaceMemberRoleService {

    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleRepository roleRepository;

    public UpdateSpaceMemberRoleService(SpaceMemberRepository spaceMemberRepository,
                                        RoleRepository roleRepository) {
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleRepository = roleRepository;
    }

    public SpaceMemberResponse execute(Long spaceId, Long userId, UpdateSpaceMemberRequest request) {
        SpaceMember member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId);
        if (member == null) {
            throw new DomainException("User is not a member of this space");
        }
        if (member.isOwner()) {
            throw new DomainException("Cannot change the role of the space owner");
        }
        Role role = roleRepository.findById(request.roleId());
        if (!role.getSpace().getId().equals(spaceId)) {
            throw new DomainException("Role must belong to the same space");
        }
        member.assignRole(role);
        SpaceMember updated = spaceMemberRepository.update(member);

        return new SpaceMemberResponse(
                updated.getId(),
                updated.getUser().getId(),
                updated.getUser().getName(),
                updated.getUser().getEmail(),
                updated.getRole().getId(),
                updated.getRole().getName(),
                updated.getJoinedAt()
        );
    }
}
