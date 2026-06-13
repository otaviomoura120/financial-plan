package com.devhouse.financial_plan.infrastructure.security

import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Family
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import spock.lang.Specification

import java.time.Instant

class SecurityServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    SecurityService securityService = new SecurityService(userRepository, endpointPermissionRepository)

    Authentication authentication = Mock()
    HttpServletRequest request = Mock()

    private User buildUserWithRole(String roleName) {
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        Role role = new Role(1L, 0, family, roleName, "desc", Instant.now(), null)
        new User(1L, 0, family, "auth0|abc", role, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
    }

    private User buildUserWithoutRole() {
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        new User(1L, 0, family, "auth0|abc", null, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
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

    def "returns false when user has no role assigned"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUserWithoutRole()

        when:
        boolean result = securityService.userHasPermissionForURL(authentication, request)

        then:
        result == false
        0 * endpointPermissionRepository._
    }

    def "returns false when no EndpointPermission matches the request"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUserWithRole("ADMIN")
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
        userRepository.findByAuth0Sub("auth0|abc") >> buildUserWithRole("ADMIN")
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
        userRepository.findByAuth0Sub("auth0|abc") >> buildUserWithRole("USER")
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

    def "uses first matching permission ordered by sequence (deny takes precedence when sequence is lower)"() {
        given:
        authentication.getName() >> "auth0|abc"
        userRepository.findByAuth0Sub("auth0|abc") >> buildUserWithRole("ADMIN")
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
