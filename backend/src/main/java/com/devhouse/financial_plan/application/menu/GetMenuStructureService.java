package com.devhouse.financial_plan.application.menu;

import com.devhouse.financial_plan.application.menu.dto.GroupMenuChildrenDto;
import com.devhouse.financial_plan.application.menu.dto.GroupMenuStructureDto;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class GetMenuStructureService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final GroupMenuRepository groupMenuRepository;

    public GetMenuStructureService(UserRepository userRepository,
                                   SpaceMemberRepository spaceMemberRepository,
                                   RoleEndpointPermissionRepository roleEndpointPermissionRepository,
                                   EndpointPermissionRepository endpointPermissionRepository,
                                   GroupMenuRepository groupMenuRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.groupMenuRepository = groupMenuRepository;
    }

    public List<GroupMenuStructureDto> execute(String auth0Sub, Long spaceId) {
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

        List<EndpointPermission> permittedPageRules = resolvePermittedRules(user, membership);

        List<GroupMenu> menus = groupMenuRepository.findAllWithChildren();

        return menus.stream()
                .map(menu -> buildStructure(menu, permittedPageRules))
                .filter(dto -> !dto.children().isEmpty())
                .toList();
    }

    private List<EndpointPermission> resolvePermittedRules(User user, SpaceMember membership) {
        List<EndpointPermission> roleRules = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(
                        Set.of(membership.getRole().getId()), EndpointPermissionType.FRONT_PAGE);

        if (!user.isMasterAdmin()) {
            return roleRules;
        }

        List<EndpointPermission> internalRules = endpointPermissionRepository
                .findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP).stream()
                .filter(ep -> ep.getType() == EndpointPermissionType.FRONT_PAGE)
                .toList();

        List<EndpointPermission> combined = new ArrayList<>(roleRules);
        combined.addAll(internalRules);
        return combined;
    }

    private GroupMenuStructureDto buildStructure(GroupMenu menu, List<EndpointPermission> permittedRules) {
        List<GroupMenuChildrenDto> accessibleChildren = menu.getChildren().stream()
                .filter(child -> isChildAccessible(child, permittedRules))
                .map(child -> new GroupMenuChildrenDto(child.getName(), child.getEndpoint(), child.getIcon()))
                .toList();

        return new GroupMenuStructureDto(menu.getName(), menu.getIcon(), accessibleChildren);
    }

    private boolean isChildAccessible(GroupMenuChildren child, List<EndpointPermission> permittedRules) {
        return permittedRules.stream()
                .anyMatch(rule -> child.getEndpoint().matches(rule.getEndpoint()));
    }
}
