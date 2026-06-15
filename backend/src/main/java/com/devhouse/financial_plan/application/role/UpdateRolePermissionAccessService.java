package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.UpdateRolePermissionAccessRequest;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateRolePermissionAccessService {

    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public UpdateRolePermissionAccessService(RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public void execute(Long roleId, Long endpointPermissionId, UpdateRolePermissionAccessRequest request) {
        RoleEndpointPermission relation = roleEndpointPermissionRepository
                .findByRoleIdAndEndpointPermissionId(roleId, endpointPermissionId);

        if (relation == null) {
            throw new DomainException("Permission relation not found for role " + roleId + " and endpoint permission " + endpointPermissionId);
        }

        if (relation.getEndpointPermission().isInternalManagement()) {
            throw new DomainException("Permissions in the internal_management group cannot be modified");
        }

        relation.setVersion(request.version());
        applyAccess(relation, request.access());
        roleEndpointPermissionRepository.update(relation);
    }

    private void applyAccess(RoleEndpointPermission relation, EndpointPermissionAccess access) {
        if (access == EndpointPermissionAccess.ALLOW) {
            relation.allow();
        } else {
            relation.deny();
        }
    }
}
