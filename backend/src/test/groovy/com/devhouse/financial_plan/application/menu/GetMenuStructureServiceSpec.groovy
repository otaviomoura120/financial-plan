package com.devhouse.financial_plan.application.menu

import com.devhouse.financial_plan.application.menu.dto.GroupMenuStructureDto
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Family
import com.devhouse.financial_plan.domain.GroupMenu
import com.devhouse.financial_plan.domain.GroupMenuChildren
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class GetMenuStructureServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    GroupMenuRepository groupMenuRepository = Mock()
    GetMenuStructureService service = new GetMenuStructureService(userRepository, endpointPermissionRepository, groupMenuRepository)

    private User buildUser(String roleName) {
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        Role role = roleName != null ? new Role(1L, 0, family, roleName, "desc", Instant.now(), null) : null
        new User(1L, 0, family, "auth0|abc", role, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
    }

    private EndpointPermission buildFrontPagePermission(String endpointRegex, String permittedRoles) {
        new EndpointPermission(1L, 0, endpointRegex, "Page", null, 1,
                EndpointPermissionType.FRONT_PAGE, "GET", permittedRoles, Instant.now(), null)
    }

    private GroupMenu buildGroupMenu(List<String> childEndpoints) {
        GroupMenu groupMenu = new GroupMenu(1L, "Finance", "icon", [], Instant.now(), null)
        List<GroupMenuChildren> children = childEndpoints.collect { endpoint ->
            new GroupMenuChildren(1L, "Page", endpoint, "icon", groupMenu, Instant.now(), null)
        }
        groupMenu.setChildren(children)
        return groupMenu
    }

    def "returns empty list when user not found"() {
        given:
        userRepository.findByAuth0Sub("auth0|unknown") >> null

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|unknown")

        then:
        result.isEmpty()
        0 * endpointPermissionRepository._
        0 * groupMenuRepository._
    }

    def "returns empty list when user has no role"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(null)

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc")

        then:
        result.isEmpty()
        0 * endpointPermissionRepository._
        0 * groupMenuRepository._
    }

    def "returns full menu when all children are permitted"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser("ADMIN")
        endpointPermissionRepository.findByType(EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions", "ADMIN,USER")
        ]
        groupMenuRepository.findAllWithChildren() >> [buildGroupMenu(["/finance/transactions"])]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc")

        then:
        result.size() == 1
        result[0].name() == "Finance"
        result[0].children().size() == 1
        result[0].children()[0].endpoint() == "/finance/transactions"
    }

    def "filters out children not matching any permitted permission"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser("ADMIN")
        endpointPermissionRepository.findByType(EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/transactions", "ADMIN")
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/finance/reports"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc")

        then:
        result.size() == 1
        result[0].children().size() == 1
        result[0].children()[0].endpoint() == "/finance/transactions"
    }

    def "excludes groups where no children are accessible"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser("USER")
        endpointPermissionRepository.findByType(EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/admin/.*", "ADMIN")
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc")

        then:
        result.isEmpty()
    }

    def "uses regex pattern matching for endpoints"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser("ADMIN")
        endpointPermissionRepository.findByType(EndpointPermissionType.FRONT_PAGE) >> [
                buildFrontPagePermission("/finance/.*", "ADMIN")
        ]
        groupMenuRepository.findAllWithChildren() >> [
                buildGroupMenu(["/finance/transactions", "/finance/reports", "/admin/roles"])
        ]

        when:
        List<GroupMenuStructureDto> result = service.execute("auth0|abc")

        then:
        result.size() == 1
        result[0].children().size() == 2
        result[0].children()*.endpoint().containsAll(["/finance/transactions", "/finance/reports"])
    }
}
