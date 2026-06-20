package com.devhouse.financial_plan.infrastructure.security;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final RoleRepository roleRepository;

    public SecurityService(UserRepository userRepository, SpaceMemberRepository spaceMemberRepository,
                           RoleEndpointPermissionRepository roleEndpointPermissionRepository,
                           EndpointPermissionRepository endpointPermissionRepository,
                           RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.roleRepository = roleRepository;
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

        String spaceIdHeader = request.getHeader("X-Space-Id");
        if (spaceIdHeader == null) {
            return false;
        }

        Long spaceId = Long.parseLong(spaceIdHeader);
        SpaceMember membership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, user.getId());
        if (membership == null) {
            return false;
        }

        List<EndpointPermission> allowedPermissions = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(
                        Set.of(membership.getRole().getId()), EndpointPermissionType.API);

        return allowedPermissions.stream().anyMatch(p -> p.matchesRequest(method, path));
    }

    /**
     * Like userHasPermissionForURL, but scoped to a single Space: only the role the user holds
     * in that specific Space is considered, instead of the union of roles across every Space
     * the user belongs to.
     */
    public boolean userHasPermissionInSpace(Authentication authentication, HttpServletRequest request, Long spaceId) {
        if (spaceId == null) {
            return false;
        }

        User user = userRepository.findByAuth0Sub(authentication.getName());
        if (user == null) {
            return false;
        }

        SpaceMember membership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, user.getId());
        if (membership == null) {
            return false;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        List<EndpointPermission> allowedPermissions = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(Set.of(membership.getRole().getId()), EndpointPermissionType.API);

        return allowedPermissions.stream().anyMatch(p -> p.matchesRequest(method, path));
    }

    /**
     * Like userHasPermissionInSpace, but resolves the Space from a Role id instead of receiving
     * it directly — for endpoints that only know which Role they're operating on (e.g. /roles/{id}).
     */
    public boolean userHasPermissionForRole(Authentication authentication, HttpServletRequest request, Long roleId) {
        Role role = roleRepository.findById(roleId);

        return role != null && userHasPermissionInSpace(authentication, request, role.getSpace().getId());
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

}
