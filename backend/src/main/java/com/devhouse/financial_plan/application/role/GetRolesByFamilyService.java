package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRolesByFamilyService {

    private final RoleRepository roleRepository;

    public GetRolesByFamilyService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleResponse> execute(Long familyId) {
        List<Role> roles = roleRepository.findByFamilyId(familyId);
        return roles.stream().map(this::toResponse).toList();
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
