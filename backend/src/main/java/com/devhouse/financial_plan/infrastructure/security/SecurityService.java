package com.devhouse.financial_plan.infrastructure.security;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;

    public SecurityService(UserRepository userRepository, SpaceMemberRepository spaceMemberRepository,
                           RoleEndpointPermissionRepository roleEndpointPermissionRepository,
                           EndpointPermissionRepository endpointPermissionRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public boolean userHasPermissionForURL(Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByAuth0Sub(authentication.getName());
        if (user == null) {
            return false;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        if (isInternalManagementRequest(method, path)) {
            return user.isMasterAdmin();
        }

        List<SpaceMember> memberships = spaceMemberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return false;
        }

        Set<Long> roleIds = extractRoleIds(memberships);
        List<EndpointPermission> allowedPermissions = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(roleIds, EndpointPermissionType.API);

        return allowedPermissions.stream().anyMatch(p -> p.matchesRequest(method, path));
    }

    public boolean isSelf(Authentication authentication, Long userId) {
        User user = userRepository.findByAuth0Sub(authentication.getName());

        return user != null && user.getId().equals(userId);
    }

    private boolean isInternalManagementRequest(String method, String path) {
        List<EndpointPermission> internalPermissions = endpointPermissionRepository
                .findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP);
        return internalPermissions.stream().anyMatch(p -> p.matchesRequest(method, path));
    }

    private Set<Long> extractRoleIds(List<SpaceMember> memberships) {
        return memberships.stream()
                .map(m -> m.getRole().getId())
                .collect(Collectors.toSet());
    }
}
