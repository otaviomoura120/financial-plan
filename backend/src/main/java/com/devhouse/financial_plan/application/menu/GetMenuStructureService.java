package com.devhouse.financial_plan.application.menu;

import com.devhouse.financial_plan.application.menu.dto.GroupMenuChildrenDto;
import com.devhouse.financial_plan.application.menu.dto.GroupMenuStructureDto;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GetMenuStructureService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;
    private final GroupMenuRepository groupMenuRepository;

    public GetMenuStructureService(UserRepository userRepository,
                                   SpaceMemberRepository spaceMemberRepository,
                                   RoleEndpointPermissionRepository roleEndpointPermissionRepository,
                                   GroupMenuRepository groupMenuRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
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

        List<EndpointPermission> allPermittedRules = roleEndpointPermissionRepository
                .findAllowedEndpointPermissionsByRoleIdsAndType(Set.of(membership.getRole().getId()), EndpointPermissionType.FRONT_PAGE);
        List<EndpointPermission> permittedPageRules = filterByMasterAdmin(user, allPermittedRules);

        List<GroupMenu> menus = groupMenuRepository.findAllWithChildren();

        return menus.stream()
                .map(menu -> buildStructure(menu, permittedPageRules))
                .filter(dto -> !dto.children().isEmpty())
                .toList();
    }

    private List<EndpointPermission> filterByMasterAdmin(User user, List<EndpointPermission> rules) {
        if (user.isMasterAdmin()) {
            return rules;
        }
        return rules.stream().filter(p -> !p.isInternalManagement()).toList();
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
