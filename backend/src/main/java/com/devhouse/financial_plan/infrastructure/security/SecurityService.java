package com.devhouse.financial_plan.infrastructure.security;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;

    public SecurityService(UserRepository userRepository, EndpointPermissionRepository endpointPermissionRepository) {
        this.userRepository = userRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
    }

    public boolean userHasPermissionForURL(Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByAuth0Sub(authentication.getName());
        if (user == null || user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().getName();
        String method = request.getMethod();
        String path = request.getRequestURI();

        List<EndpointPermission> permissions = endpointPermissionRepository.findByType(EndpointPermissionType.API);
        return permissions.stream()
                .filter(p -> p.matchesRequest(method, path))
                .findFirst()
                .map(p -> p.isPermitted(roleName))
                .orElse(false);
    }
}
