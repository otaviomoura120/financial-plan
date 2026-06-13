package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.application.role.dto.UpdateRoleRequest;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateRoleService {

    private final RoleRepository roleRepository;

    public UpdateRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public RoleResponse execute(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id);
        role.setVersion(request.version());
        role.update(request.name(), request.description());
        role.validate();
        Role updated = roleRepository.update(role);
        return toResponse(updated);
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
