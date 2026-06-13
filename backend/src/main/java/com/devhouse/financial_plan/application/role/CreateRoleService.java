package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateRoleService {

    private final RoleRepository roleRepository;
    private final SpaceRepository spaceRepository;

    public CreateRoleService(RoleRepository roleRepository, SpaceRepository spaceRepository) {
        this.roleRepository = roleRepository;
        this.spaceRepository = spaceRepository;
    }

    public RoleResponse execute(CreateRoleRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        Role role = new Role(null, 0, space, request.name(), request.description(), Instant.now(), null);
        role.validate();
        Role saved = roleRepository.save(role);
        return toResponse(saved);
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
