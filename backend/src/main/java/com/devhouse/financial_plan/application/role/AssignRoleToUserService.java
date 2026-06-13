package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class AssignRoleToUserService {

    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleRepository roleRepository;

    public AssignRoleToUserService(SpaceMemberRepository spaceMemberRepository, RoleRepository roleRepository) {
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleRepository = roleRepository;
    }

    public void execute(Long userId, Long roleId, Long spaceId) {
        SpaceMember member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId);
        if (member == null) {
            throw new DomainException("User is not a member of this space");
        }
        Role role = roleRepository.findById(roleId);
        if (!role.getSpace().getId().equals(spaceId)) {
            throw new DomainException("Role must belong to the same space as the user's membership");
        }
        member.assignRole(role);
        spaceMemberRepository.update(member);
    }
}
