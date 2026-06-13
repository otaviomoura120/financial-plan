package com.devhouse.financial_plan.infrastructure.security

import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import spock.lang.Specification

import java.time.Instant

class SecurityServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    SecurityService securityService = new SecurityService(userRepository, spaceMemberRepository, endpointPermissionRepository)

    Authentication authentication = Mock()
    HttpServletRequest request = Mock()

    private User buildUser() {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
    }

    private SpaceMember buildMemberWithRole(String roleName) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Role role = new Role(1L, 0, space, roleName, "desc", Instant.now(), null)
        User user = buildUser()
        new SpaceMember(1L, space, user, role, Instant.now())
    }

    private EndpointPermission buildPermission(String endpoint, String methods, String roles) {
        new EndpointPermission(1L, 0, endpoint, "Test", null, 1, EndpointPermissionType.API,
                methods, roles, Instant.now(), null)
    }

    def "returns false when user is not found in database"() {
        given:
        authentication.getName() >> "auth0|unknown"
        userRepository.findByAuth0Sub("auth0|unknown") >> null

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * endpointPermissionRepository._
    }

    def "returns false when user has no space memberships"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> []

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * endpointPermissionRepository._
    }

    def "returns false when no EndpointPermission matches the request"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole("ADMIN")]
        request.getMethod() >> "DELETE"
        request.getRequestURI() >> "/roles/1"
        endpointPermissionRepository.findByType(EndpointPermissionType.API) >> [
                buildPermission("/roles.*", "GET,POST", "ADMIN")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
    }

    def "returns true when matching permission allows user role"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole("ADMIN")]
        request.getMethod() >> "GET"
        request.getRequestURI() >> "/roles"
        endpointPermissionRepository.findByType(EndpointPermissionType.API) >> [
                buildPermission("/roles.*", "GET,POST", "ADMIN,MANAGER")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == true
    }

    def "returns false when matching permission does not include user role"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole("USER")]
        request.getMethod() >> "DELETE"
        request.getRequestURI() >> "/roles/1"
        endpointPermissionRepository.findByType(EndpointPermissionType.API) >> [
                buildPermission("/roles.*", "DELETE", "ADMIN")
        ]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
    }

    def "uses first matching permission ordered by sequence"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser()
        spaceMemberRepository.findByUserId(1L) >> [buildMemberWithRole("ADMIN")]
        request.getMethod() >> "DELETE"
        request.getRequestURI() >> "/roles/1"
        EndpointPermission denialRule = new EndpointPermission(1L, 0, "/roles.*", "Deny All", null, 1,
                EndpointPermissionType.API, "DELETE", "SUPERADMIN", Instant.now(), null)
        EndpointPermission allowRule = new EndpointPermission(2L, 0, "/roles.*", "Allow Admin", null, 2,
                EndpointPermissionType.API, "DELETE", "ADMIN", Instant.now(), null)
        endpointPermissionRepository.findByType(EndpointPermissionType.API) >> [denialRule, allowRule]

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
    }
}
