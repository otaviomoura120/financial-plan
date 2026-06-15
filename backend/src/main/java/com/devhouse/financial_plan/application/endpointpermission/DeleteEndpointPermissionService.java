package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteEndpointPermissionService {

    private final EndpointPermissionRepository endpointPermissionRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public DeleteEndpointPermissionService(EndpointPermissionRepository endpointPermissionRepository,
                                           RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public void execute(Long id) {
        endpointPermissionRepository.findById(id);
        roleEndpointPermissionRepository.deleteByEndpointPermissionId(id);
        endpointPermissionRepository.delete(id);
    }
}
