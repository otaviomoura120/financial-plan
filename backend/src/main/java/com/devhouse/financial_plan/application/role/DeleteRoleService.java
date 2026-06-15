package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteRoleService {

    private final RoleRepository roleRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public DeleteRoleService(RoleRepository roleRepository,
                             RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.roleRepository = roleRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public void execute(Long id) {
        roleRepository.findById(id);
        roleEndpointPermissionRepository.deleteByRoleId(id);
        roleRepository.delete(id);
    }
}
