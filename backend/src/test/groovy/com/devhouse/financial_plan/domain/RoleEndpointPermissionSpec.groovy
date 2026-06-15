package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import spock.lang.Specification

import java.time.Instant

class RoleEndpointPermissionSpec extends Specification {

    private Role buildRole() {
        Space space = new Space(1L, 0, "Space", null, Instant.now(), null)
        new Role(1L, 0, space, "ADMIN", null, Instant.now(), null)
    }

    private EndpointPermission buildEndpointPermission() {
        new EndpointPermission(1L, 0, "/roles.*", "Roles", null, 1,
                EndpointPermissionType.API, "GET", Instant.now(), null)
    }

    private RoleEndpointPermission buildRelation(EndpointPermissionAccess access) {
        new RoleEndpointPermission(1L, 0, buildRole(), buildEndpointPermission(), access, Instant.now(), null)
    }

    def "isAllowed returns true when permission is ALLOW"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.ALLOW)

        expect:
        relation.isAllowed() == true
    }

    def "isAllowed returns false when permission is DENY"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.DENY)

        expect:
        relation.isAllowed() == false
    }

    def "allow() changes permission to ALLOW and sets updatedAt"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.DENY)

        when:
        relation.allow()

        then:
        relation.isAllowed() == true
        relation.getUpdatedAt() != null
    }

    def "deny() changes permission to DENY and sets updatedAt"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.ALLOW)

        when:
        relation.deny()

        then:
        relation.isAllowed() == false
        relation.getUpdatedAt() != null
    }

    def "validate throws DomainException when role is null"() {
        given:
        RoleEndpointPermission relation = new RoleEndpointPermission(null, 0, null,
                buildEndpointPermission(), EndpointPermissionAccess.DENY, Instant.now(), null)

        when:
        relation.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate throws DomainException when endpointPermission is null"() {
        given:
        RoleEndpointPermission relation = new RoleEndpointPermission(null, 0, buildRole(),
                null, EndpointPermissionAccess.DENY, Instant.now(), null)

        when:
        relation.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate throws DomainException when permission is null"() {
        given:
        RoleEndpointPermission relation = new RoleEndpointPermission(null, 0, buildRole(),
                buildEndpointPermission(), null, Instant.now(), null)

        when:
        relation.validate()

        then:
        thrown(com.devhouse.financial_plan.domain.exception.DomainException)
    }

    def "validate passes for a valid relation"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.DENY)

        when:
        relation.validate()

        then:
        noExceptionThrown()
    }

    def "setVersion throws ObjectOptimisticLockingFailureException on version mismatch"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.DENY)

        when:
        relation.setVersion(99)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
    }
}
