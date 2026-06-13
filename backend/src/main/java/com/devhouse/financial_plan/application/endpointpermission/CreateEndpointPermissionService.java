package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.application.endpointpermission.dto.CreateEndpointPermissionRequest;
import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateEndpointPermissionService {

    private final EndpointPermissionRepository endpointPermissionRepository;

    public CreateEndpointPermissionService(EndpointPermissionRepository endpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public EndpointPermissionResponse execute(CreateEndpointPermissionRequest request) {
        EndpointPermission permission = new EndpointPermission(null, 0, request.endpoint(), request.name(),
                request.icon(), request.sequence(), request.type(),
                request.permittedMethods(), request.permittedRoles(), Instant.now(), null);
        permission.validate();
        EndpointPermission saved = endpointPermissionRepository.save(permission);
        return toResponse(saved);
    }

    private EndpointPermissionResponse toResponse(EndpointPermission p) {
        return new EndpointPermissionResponse(
                p.getId(), p.getVersion(), p.getEndpoint(), p.getName(), p.getIcon(),
                p.getSequence(), p.getType(), p.getPermittedMethods(), p.getPermittedRoles(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
