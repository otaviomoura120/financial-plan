package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.RoleEndpointPermissionResponse;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRolePermissionsService {

    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public GetRolePermissionsService(RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public List<RoleEndpointPermissionResponse> execute(Long roleId) {
        return roleEndpointPermissionRepository.findByRoleId(roleId).stream()
                .map(this::toResponse)
                .toList();
    }

    private RoleEndpointPermissionResponse toResponse(RoleEndpointPermission rep) {
        return new RoleEndpointPermissionResponse(
                rep.getId(),
                rep.getVersion(),
                rep.getEndpointPermission().getId(),
                rep.getEndpointPermission().getName(),
                rep.getEndpointPermission().getEndpoint(),
                rep.getEndpointPermission().getType(),
                rep.getPermission()
        );
    }
}
