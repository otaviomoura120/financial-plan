package com.devhouse.financial_plan.application.menu

import com.devhouse.financial_plan.application.menu.dto.GroupMenuStructureDto
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.GroupMenu
import com.devhouse.financial_plan.domain.GroupMenuChildren
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class GetMenuStructureServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    GroupMenuRepository groupMenuRepository = Mock()
    GetMenuStructureService service = new GetMenuStructureService(userRepository, spaceMemberRepository, roleEndpointPermissionRepository, groupMenuRepository)

    private User buildUser(boolean masterAdmin = false) {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, masterAdmin)
    }

    private SpaceMember buildMemberWithRole(Long roleId, String roleName, Long spaceId = 1L) {
        Space space = new Space(spaceId, 0, "My Space", null, Instant.now(), null)
        Role role = new Role(roleId, 0, space, roleName, "desc", Instant.now(), null)
        new SpaceMember(1L, 0, space, buildUser(), role, Instant.now())
    }

    private EndpointPermission buildFrontPagePermission(String endpointRegex, String group = "Menu") {
        new EndpointPermission(1L, 0, endpointRegex, "Page", null, 1,
                EndpointPermissionType.FRONT_PAGE, "GET", group, Instant.now(), null)
    }

    private GroupMenu buildGroupMenu(List<String> childEndpoints) {
        GroupMenu groupMenu = new GroupMenu(1L, 0, "Finance", "icon", [], Instant.now(), null)
        List<GroupMenuChildren> children = childEndpoints.collect { endpoint ->
            new GroupMenuChildren(1L, 0, "Page", endpoint, "icon", groupMenu, Instant.now(), null)
        }
        groupMenu.setChildren(children)
        return groupMenu
    }

    def "returns empty list when user not found"() {
        given:
        userRepository.findByAuth0Sub("auth0|unknown") >> null

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|unknown", 1L)

        then:
        result.isEmpty()
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
        0 * groupMenuRepository._
    }

    def "returns empty list when spaceId is null"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", null)

        then:
        result.isEmpty()
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
        0 * groupMenuRepository._
    }

    def "returns empty list when user is not a member of the requested space"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> null

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.isEmpty()
        0 * roleEndpointPermissionRepository._
        0 * groupMenuRepository._
    }

    def "returns full menu when all children are permitted"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions")
        ]
        groupMenuRepository.findAllWithChildren() >> [buildGroupMenu(["/finance/transactions"])]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.size() == 1
        result[0].name() == "Finance"
        result[0].children().size() == 1
        result[0].children()[0].endpoint() == "/finance/transactions"
    }

    def "filters out children not matching any permitted permission"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions")
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/finance/reports"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.size() == 1
        result[0].children().size() == 1
        result[0].children()[0].endpoint() == "/finance/transactions"
    }

    def "excludes groups where no children are accessible"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "MEMBER")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> []
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.isEmpty()
    }

    def "hides internal_management menu items for non-MASTER_ADMIN users"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions"),
                buildFrontPagePermission("/endpoint-permissions", EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/endpoint-permissions"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.size() == 1
        result[0].children().size() == 1
        result[0].children()[0].endpoint() == "/finance/transactions"
    }

    def "shows internal_management menu items for MASTER_ADMIN users"() {
        given:
        userRepository.findByAuth0Sub("auth0|master") >> buildUser(true)
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions"),
                buildFrontPagePermission("/endpoint-permissions", EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/endpoint-permissions"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|master", 1L)

        then:
        result.size() == 1
        result[0].children().size() == 2
    }

    def "uses regex pattern matching for endpoints"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/.*")
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/finance/reports", "/admin/roles"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 1L)

        then:
        result.size() == 1
        result[0].children().size() == 2
        result[0].children()*.endpoint().containsAll(["/finance/transactions", "/finance/reports"])
    }

    def "uses the role from the requested space, not other spaces the user belongs to"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(2L, 1L) >> buildMemberWithRole(20L, "MEMBER", 2L)
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [20L] as Set, EndpointPermissionType.FRONT_PAGE) >> []
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc", 2L)

        then:
        result.isEmpty()
        0 * spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L)
    }
}
