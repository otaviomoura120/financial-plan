package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.application.role.dto.RoleEndpointPermissionResponse;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetRolePermissionsService {

    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;
    private final UserRepository userRepository;

    public GetRolePermissionsService(RoleEndpointPermissionRepository roleEndpointPermissionRepository,
                                     UserRepository userRepository) {
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
        this.userRepository = userRepository;
    }

    public List<RoleEndpointPermissionResponse> execute(Long roleId, String callerAuth0Sub) {
        User caller = userRepository.findByAuth0Sub(callerAuth0Sub);
        boolean isMasterAdmin = caller != null && caller.isMasterAdmin();
        return roleEndpointPermissionRepository.findByRoleId(roleId).stream()
                .filter(rep -> isMasterAdmin || !rep.getEndpointPermission().isInternalManagement())
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
                rep.getEndpointPermission().getGroup(),
                rep.getPermission()
        );
    }
}
