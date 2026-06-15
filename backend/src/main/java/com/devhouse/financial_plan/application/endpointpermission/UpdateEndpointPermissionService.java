package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.application.endpointpermission.dto.UpdateEndpointPermissionRequest;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateEndpointPermissionService {

    private final EndpointPermissionRepository endpointPermissionRepository;

    public UpdateEndpointPermissionService(EndpointPermissionRepository endpointPermissionRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public EndpointPermissionResponse execute(Long id, UpdateEndpointPermissionRequest request) {
        EndpointPermission permission = endpointPermissionRepository.findById(id);
        permission.setVersion(request.version());
        permission.update(request.endpoint(), request.name(), request.icon(), request.sequence(),
                request.type(), request.permittedMethods(), request.group());
        permission.validate();
        EndpointPermission updated = endpointPermissionRepository.update(permission);
        return toResponse(updated);
    }

    private EndpointPermissionResponse toResponse(EndpointPermission p) {
        return new EndpointPermissionResponse(
                p.getId(), p.getVersion(), p.getEndpoint(), p.getName(), p.getIcon(),
                p.getSequence(), p.getType(), p.getPermittedMethods(), p.getGroup(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
