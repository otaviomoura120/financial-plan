package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.application.endpointpermission.dto.CreateEndpointPermissionRequest;
import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CreateEndpointPermissionService {

    private final EndpointPermissionRepository endpointPermissionRepository;
    private final RoleRepository roleRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public CreateEndpointPermissionService(EndpointPermissionRepository endpointPermissionRepository,
                                           RoleRepository roleRepository,
                                           RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.roleRepository = roleRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public EndpointPermissionResponse execute(CreateEndpointPermissionRequest request) {
        EndpointPermission permission = new EndpointPermission(null, 0, request.endpoint(), request.name(),
                request.icon(), request.sequence(), request.type(), request.permittedMethods(), request.group(), Instant.now(), null);
        permission.validate();
        EndpointPermission saved = endpointPermissionRepository.save(permission);
        createDefaultPermissions(saved);
        return toResponse(saved);
    }

    private void createDefaultPermissions(EndpointPermission endpointPermission) {
        Instant now = Instant.now();
        List<Role> allRoles = roleRepository.findAll();
        List<RoleEndpointPermission> relations = allRoles.stream()
                .map(role -> new RoleEndpointPermission(null, 0, role, endpointPermission, EndpointPermissionAccess.DENY, now, null))
                .toList();
        roleEndpointPermissionRepository.saveAll(relations);
    }

    private EndpointPermissionResponse toResponse(EndpointPermission p) {
        return new EndpointPermissionResponse(
                p.getId(), p.getVersion(), p.getEndpoint(), p.getName(), p.getIcon(),
                p.getSequence(), p.getType(), p.getPermittedMethods(), p.getGroup(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
