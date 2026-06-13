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
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetMenuStructureService {

    private final UserRepository userRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final GroupMenuRepository groupMenuRepository;

    public GetMenuStructureService(UserRepository userRepository,
                                   SpaceMemberRepository spaceMemberRepository,
                                   EndpointPermissionRepository endpointPermissionRepository,
                                   GroupMenuRepository groupMenuRepository) {
        this.userRepository = userRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.groupMenuRepository = groupMenuRepository;
    }

    public List<GroupMenuStructureDto> execute(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            return List.of();
        }

        List<SpaceMember> memberships = spaceMemberRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return List.of();
        }

        Set<String> roleNames = memberships.stream()
                .map(m -> m.getRole().getName())
                .collect(Collectors.toSet());

        List<EndpointPermission> permittedPageRules = findPermittedPageRules(roleNames);
        List<GroupMenu> menus = groupMenuRepository.findAllWithChildren();

        return menus.stream()
                .map(menu -> buildStructure(menu, permittedPageRules))
                .filter(dto -> !dto.children().isEmpty())
                .toList();
    }

    private List<EndpointPermission> findPermittedPageRules(Set<String> roleNames) {
        return endpointPermissionRepository.findByType(EndpointPermissionType.FRONT_PAGE)
                .stream()
                .filter(p -> roleNames.stream().anyMatch(p::isPermitted))
                .toList();
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
