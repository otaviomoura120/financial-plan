package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CreateRoleService {

    private final RoleRepository roleRepository;
    private final SpaceRepository spaceRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public CreateRoleService(RoleRepository roleRepository, SpaceRepository spaceRepository,
                             EndpointPermissionRepository endpointPermissionRepository,
                             RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.roleRepository = roleRepository;
        this.spaceRepository = spaceRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public RoleResponse execute(CreateRoleRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        Role role = new Role(null, 0, space, request.name(), request.description(), Instant.now(), null);
        role.validate();
        Role saved = roleRepository.save(role);
        createDefaultPermissions(saved);
        return toResponse(saved);
    }

    private void createDefaultPermissions(Role role) {
        Instant now = Instant.now();
        List<EndpointPermission> allPermissions = endpointPermissionRepository.findAll();
        List<RoleEndpointPermission> relations = allPermissions.stream()
                .filter(ep -> !ep.isInternalManagement())
                .map(ep -> new RoleEndpointPermission(null, 0, role, ep, EndpointPermissionAccess.DENY, now, null))
                .toList();
        roleEndpointPermissionRepository.saveAll(relations);
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
