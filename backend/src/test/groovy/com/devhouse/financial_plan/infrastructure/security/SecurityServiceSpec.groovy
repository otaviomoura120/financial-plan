package com.devhouse.financial_plan.infrastructure.security

import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import spock.lang.Specification

import java.time.Instant

class SecurityServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    SecurityService securityService = new SecurityService(userRepository, spaceMemberRepository,
            roleEndpointPermissionRepository, endpointPermissionRepository)

    Authentication authentication = Mock()
    HttpServletRequest request = Mock()

    private User buildUser(boolean masterAdmin = false) {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, masterAdmin)
    }

    private SpaceMember buildMemberWithRole(Long roleId, String roleName) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Role role = new Role(roleId, 0, space, roleName, "desc", Instant.now(), null)
        new SpaceMember(1L, space, buildUser(), role, Instant.now())
    }

    private EndpointPermission buildPermission(String endpoint, String methods, String group = "Test") {
        new EndpointPermission(1L, 0, endpoint, "Test", null, 1, EndpointPermissionType.API,
                methods, group, Instant.now(), null)
    }

    def "returns false when user is not found in database"() {
        given:
        authentication.getName() >> "auth0|unknown"
        userRepository.findByAuth0Sub("auth0|unknown") >> null

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * roleEndpointPermissionRepository._
    }

    def "returns false when user has no space memberships"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/roles"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> []
        spaceMemberRepository.findByUserId(1L) >> []

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * roleEndpointPermissionRepository._
    }

    def "returns false when no ALLOW permission matches the request"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole(10L, "ADMIN")]
        request.getMethod() >> "DELETE"
        request.getRequestURI() >> "/roles/1"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> []
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.API) >> [
                buildPermission("/roles.*", "GET,POST")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
    }

    def "returns true when an ALLOW permission matches the request method and path"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole(10L, "ADMIN")]
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/roles"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> []
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.API) >> [
                buildPermission("/roles.*", "GET,POST")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == true
    }

    def "returns false when no ALLOW permissions exist for the user roles"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole(10L, "MEMBER")]
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/roles"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> []
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L] as Set, EndpointPermissionType.API) >> []

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
    }

    def "collects role IDs from all memberships when user belongs to multiple spaces"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [
                buildMemberWithRole(10L, "ADMIN"),
                buildMemberWithRole(20L, "MEMBER")
        ]
        request.getMethod() >> "DELETE"
        request.getRequestURI() >> "/roles/5"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> []
        roleEndpointPermissionRepository.findAllowedEndpointPermissionsByRoleIdsAndType(
                [10L, 20L] as Set, EndpointPermissionType.API) >> [
                buildPermission("/roles/[0-9]+", "DELETE")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == true
    }

    def "returns true for internal_management endpoint when user is MASTER_ADMIN"() {
        given:
        authentication.getName() >> "auth0|master"
        userRepository.findByAuth0Sub("auth0|master") >> buildUser(true)
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/endpoint-permissions"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> [
                buildPermission("/endpoint-permissions", "GET", EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == true
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
    }

    def "returns false for internal_management endpoint when user is not MASTER_ADMIN"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/endpoint-permissions"
        endpointPermissionRepository.findByGroup(EndpointPermission.INTERNAL_MANAGEMENT_GROUP) >> [
                buildPermission("/endpoint-permissions", "GET", EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * spaceMemberRepository._
        0 * roleEndpointPermissionRepository._
    }
}
