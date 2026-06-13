package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateRoleService {

    private final RoleRepository roleRepository;
    private final FamilyRepository familyRepository;

    public CreateRoleService(RoleRepository roleRepository, FamilyRepository familyRepository) {
        this.roleRepository = roleRepository;
        this.familyRepository = familyRepository;
    }

    public RoleResponse execute(CreateRoleRequest request) {
        Family family = familyRepository.findById(request.familyId());
        Role role = new Role(null, 0, family, request.name(), request.description(), Instant.now(), null);
        role.validate();
        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getVersion(),
                role.getFamily().getId(),
                role.getName(),
                role.getDescription(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
