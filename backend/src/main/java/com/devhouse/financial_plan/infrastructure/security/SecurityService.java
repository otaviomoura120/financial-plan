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

    public static final String SPACE_HEADER = "X-Space-Id";

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

        Long spaceId = parseSpaceHeader(request.getHeader(SPACE_HEADER));
        if (spaceId == null) {
            return false;
        }

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
        return hasApiPermissionInSpace(authentication, spaceId, request.getMethod(), request.getRequestURI());
    }

    /**
     * Grants an endpoint to whoever may send invites in the Space, without a permission row of its own:
     * the user's role in that Space must be allowed to call POST /spaces/{spaceId}/invites.
     * This is a business rule, not an anti-enumeration measure: every user owns the Space created at onboarding.
     */
    public boolean userCanInviteToSpace(Authentication authentication, Long spaceId) {
        return hasApiPermissionInSpace(authentication, spaceId, "POST", "/spaces/" + spaceId + "/invites");
    }

    private boolean hasApiPermissionInSpace(Authentication authentication, Long spaceId, String method, String path) {
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

    private Long parseSpaceHeader(String spaceIdHeader) {
        if (spaceIdHeader == null) {
            return null;
        }
        try {
            return Long.parseLong(spaceIdHeader.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isInternalManagementRequest(String method, String path) {
        List<EndpointPermission> internalPermissions = endpointPermissionRepository
                .findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP);
        return internalPermissions.stream().anyMatch(p -> p.matchesRequest(method, path));
    }

}
