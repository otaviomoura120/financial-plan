package com.devhouse.financial_plan.infrastructure.security;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;

    public SecurityService(UserRepository userRepository, SpaceMemberRepository spaceMemberRepository,
                           EndpointPermissionRepository endpointPermissionRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public boolean userHasPermissionForURL(Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByAuth0Sub(authentication.getName());
        if (user == null) {
            return false;
        }

        List<SpaceMember> memberships = spaceMemberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return false;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        List<EndpointPermission> permissions = endpointPermissionRepository.findByType(EndpointPermissionType.API);
        return permissions.stream()
                .filter(p -> p.matchesRequest(method, path))
                .findFirst()
                .map(p -> memberships.stream().anyMatch(m -> p.isPermitted(m.getRole().getName())))
                .orElse(false);
    }
}
