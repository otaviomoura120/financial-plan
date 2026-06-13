package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteEndpointPermissionService {

    private final EndpointPermissionRepository endpointPermissionRepository;

    public DeleteEndpointPermissionService(EndpointPermissionRepository endpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public void execute(Long id) {
        endpointPermissionRepository.findById(id);
        endpointPermissionRepository.delete(id);
    }
}
