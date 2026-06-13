package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetEndpointPermissionsService {

    private final EndpointPermissionRepository endpointPermissionRepository;

    public GetEndpointPermissionsService(EndpointPermissionRepository endpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public List<EndpointPermissionResponse> execute() {
        List<EndpointPermission> permissions = endpointPermissionRepository.findAllOrderedBySequence();
        return permissions.stream().map(this::toResponse).toList();
    }

    private EndpointPermissionResponse toResponse(EndpointPermission p) {
        return new EndpointPermissionResponse(
                p.getId(), p.getVersion(), p.getEndpoint(), p.getName(), p.getIcon(),
                p.getSequence(), p.getType(), p.getPermittedMethods(), p.getPermittedRoles(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
