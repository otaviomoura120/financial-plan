package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class EndpointPermissionSpec extends Specification {

    private EndpointPermission buildPermission(String endpoint, String permittedMethods) {
        new EndpointPermission(1L, 0, endpoint, "Test", null, 1, EndpointPermissionType.API,
                permittedMethods, "Role", Instant.now(), null)
    }

    def "matchesRequest returns true when path matches regex and method is allowed"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST")

        expect:
        permission.matchesRequest("GET", "/roles") == true
        permission.matchesRequest("POST", "/roles/123") == true
    }

    def "matchesRequest returns false when path does not match regex"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST")

        expect:
        permission.matchesRequest("GET", "/users") == false
        permission.matchesRequest("GET", "/endpoint-permissions") == false
    }

    def "matchesRequest returns false when HTTP method is not permitted"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET")

        expect:
        permission.matchesRequest("DELETE", "/roles/1") == false
        permission.matchesRequest("PUT", "/roles/1") == false
    }

    def "matchesRequest is case-insensitive for method"() {
        given:
        EndpointPermission permission = buildPermission("/users.*", "get,post")

        expect:
        permission.matchesRequest("GET", "/users") == true
        permission.matchesRequest("POST", "/users") == true
    }

    def "validate throws DomainException when endpoint is blank"() {
        given:
        EndpointPermission permission = buildPermission("", "GET")

        when:
        permission.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when name is blank"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "", null, 1,
                EndpointPermissionType.API, "GET", "Role", Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when type is null"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "Test", null, 1,
                null, "GET", "Role", Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when group is blank"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "Test", null, 1,
                EndpointPermissionType.API, "GET", "", Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when group is null"() {
        given:
        EndpointPermission permission = new EndpointPermission(null, 0, "/roles.*", "Test", null, 1,
                EndpointPermissionType.API, "GET", null, Instant.now(), null)

        when:
        permission.validate()

        then:
        thrown(DomainException)
    }

    def "validate passes for a valid permission"() {
        given:
        EndpointPermission permission = buildPermission("/roles.*", "GET,POST")

        when:
        permission.validate()

        then:
        noExceptionThrown()
    }
}
