package com.devhouse.financial_plan.application.endpointpermission;

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetEndpointPermissionsService {

    private final EndpointPermissionRepository endpointPermissionRepository;
    private final UserRepository userRepository;

    public GetEndpointPermissionsService(EndpointPermissionRepository endpointPermissionRepository,
                                         UserRepository userRepository) {
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.userRepository = userRepository;
    }

    public List<EndpointPermissionResponse> execute(String group, String callerAuth0Sub) {
        User caller = userRepository.findByAuth0Sub(callerAuth0Sub);
        boolean isMasterAdmin = caller != null && caller.isMasterAdmin();
        List<EndpointPermission> permissions = resolvePermissions(group);
        return permissions.stream()
                .filter(p -> isMasterAdmin || !p.isInternalManagement())
                .map(this::toResponse)
                .toList();
    }

    private List<EndpointPermission> resolvePermissions(String group) {
        if (group != null && !group.isBlank()) {
            return endpointPermissionRepository.findByGroup(group);
        }
        return endpointPermissionRepository.findAllOrderedBySequence();
    }

    private EndpointPermissionResponse toResponse(EndpointPermission p) {
        return new EndpointPermissionResponse(
                p.getId(), p.getVersion(), p.getEndpoint(), p.getName(), p.getIcon(),
                p.getSequence(), p.getType(), p.getPermittedMethods(), p.getGroup(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
