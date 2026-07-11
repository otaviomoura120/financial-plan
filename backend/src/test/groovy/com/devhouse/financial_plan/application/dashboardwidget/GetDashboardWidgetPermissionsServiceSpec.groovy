package com.devhouse.financial_plan.application.dashboardwidget

import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class GetDashboardWidgetPermissionsServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    GetDashboardWidgetPermissionsService service = new GetDashboardWidgetPermissionsService(
            userRepository, spaceMemberRepository, roleEndpointPermissionRepository)

    private User buildUser() {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, false)
    }

    private SpaceMember buildMemberWithRole(Long roleId, String roleName, Long spaceId = 1L) {
        Space space = new Space(spaceId, 0, "My Space", null, Instant.now(), null)
        Role role = new Role(roleId, 0, space, roleName, "desc", Instant.now(), null)
        new SpaceMember(1L, 0, space, buildUser(), role, Instant.now())
    }

    private EndpointPermission buildWidgetPermission(String key) {
        new EndpointPermission(1L, 0, key, "Widget", null, 1,
                EndpointPermissionType.WIDGET, null, "Dashboard", Instant.now(), null)
    }

    def "returns empty list when user not found"() {
        given:
        userRepository.findByAuth0Sub("auth0|unknown") >> null

        when:
        List<String> result = service.execute("auth0|unknown", 1L)

        then:
        result.isEmpty()
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
    }

    def "returns empty list when spaceId is null"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()

        when:
        List<String> result = service.execute("auth0|abc", null)

        then:
        result.isEmpty()
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
    }

    def "returns empty list when user is not a member of the requested space"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> null

        when:
        List<String> result = service.execute("auth0|abc", 1L)

        then:
        result.isEmpty()
        0 * roleEndpointPermissionRepository._
    }

    def "returns the allowed widget keys for the user's role in the requested space"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L) >> buildMemberWithRole(10L, "ADMIN")
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.WIDGET) >> [
                buildWidgetPermission("dashboard:summary-tiles"),
                buildWidgetPermission("dashboard:due-this-week"),
        ]

        when:
        List<String> result = service.execute("auth0|abc", 1L)

        then:
        result.size() == 2
        result.containsAll(["dashboard:summary-tiles", "dashboard:due-this-week"])
    }

    def "uses the role from the requested space, not other spaces the user belongs to"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findBySpaceIdAndUserId(2L, 1L) >> buildMemberWithRole(20L, "MEMBER", 2L)
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [20L] as Set, EndpointPermissionType.WIDGET) >> []

        when:
        List<String> result = service.execute("auth0|abc", 2L)

        then:
        result.isEmpty()
        0 * spaceMemberRepository.findBySpaceIdAndUserId(1L, 1L)
    }
}
