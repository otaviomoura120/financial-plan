package com.devhouse.financial_plan.application.dashboardwidget;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GetDashboardWidgetPermissionsService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public GetDashboardWidgetPermissionsService(UserRepository userRepository,
                                                 SpaceMemberRepository spaceMemberRepository,
                                                 RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public List<String> execute(String auth0Sub, Long spaceId) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            return List.of();
        }

        SpaceMember membership = spaceId != null
                ? spaceMemberRepository.findBySpaceIdAndUserId(spaceId, user.getId())
                : null;
        if (membership == null) {
            return List.of();
        }

        List<EndpointPermission> allowedWidgets = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(
                        Set.of(membership.getRole().getId()), EndpointPermissionType.WIDGET);

        return allowedWidgets.stream()
                .map(EndpointPermission::getEndpoint)
                .toList();
    }
}
