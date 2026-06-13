package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRolesBySpaceService {

    private final RoleRepository roleRepository;

    public GetRolesBySpaceService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> execute(Long spaceId) {
        List<Role> roles = roleRepository.findBySpaceId(spaceId);
        return roles.stream().map(this::toResponse).toList();
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getVersion(),
                role.getSpace().getId(),
                role.getName(),
                role.getDescription(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
