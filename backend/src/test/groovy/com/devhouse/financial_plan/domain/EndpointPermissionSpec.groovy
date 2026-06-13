package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import spock.lang.Specification

import java.time.Instant

class EndpointPermissionSpec extends Specification {

    private EndpointPermission buildPermission(String endpoint, String permittedMethods, String permittedRoles) {
        new EndpointPermission(1L, 0, endpoint, "Test", null, 1, EndpointPermissionType.API,
                permittedMethods, permittedRoles, Instant.now(), null)
    }

    def "matchesRequest returns true when path matches regex and method is allowed"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST", "ADMIN")

        expect:
        permission.matchesRequest("GET", "/roles") == true
        permission.matchesRequest("POST", "/roles/123") == true
    }

    def "matchesRequest returns false when path does not match regex"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST", "ADMIN")

        expect:
        permission.matchesRequest("GET", "/users") == false
        permission.matchesRequest("GET", "/endpoint-permissions") == false
    }

    def "matchesRequest returns false when HTTP method is not permitted"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET", "ADMIN")

        expect:
        permission.matchesRequest("DELETE", "/roles/1") == false
        permission.matchesRequest("PUT", "/roles/1") == false
    }

    def "matchesRequest is case-insensitive for method"() {
        given:
        EndpointPermission permission = buildPermission("/users.*", "get,post", "ADMIN")

        expect:
        permission.matchesRequest("GET", "/users") == true
        permission.matchesRequest("POST", "/users") == true
    }

    def "isPermitted returns true when role is in permittedRoles CSV"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET", "ADMIN,MANAGER")

        expect:
        permission.isPermitted("ADMIN") == true
        permission.isPermitted("MANAGER") == true
    }

    def "isPermitted returns false when role is not in permittedRoles CSV"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET", "ADMIN,MANAGER")

        expect:
        permission.isPermitted("USER") == false
        permission.isPermitted("GUEST") == false
    }

    def "isPermitted trims whitespace around role names"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET", "ADMIN , MANAGER")

        expect:
        permission.isPermitted("ADMIN") == true
        permission.isPermitted("MANAGER") == true
    }

    def "validate throws DomainException when endpoint is blank"() {
        given:
        EndpointPermission permission = buildPermission("", "GET", "ADMIN")

        when:
        permission.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate throws DomainException when name is blank"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "", null, 1,
                EndpointPermissionType.API, "GET", "ADMIN", Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate throws DomainException when type is null"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "Test", null, 1,
                null, "GET", "ADMIN", Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate passes for a valid permission"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST", "ADMIN")

        when:
        permission.validate()

        then:
        noExceptionThrown()
    }
}
