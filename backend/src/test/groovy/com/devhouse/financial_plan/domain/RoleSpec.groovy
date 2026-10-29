package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class RoleSpec extends Specification {

    private Role buildRole(String name) {
        new Role(1L, 0, new Space(1L, 0, "My Space", null, Instant.now(), null), name, "description", Instant.now(), null)
    }

    def "rejects the reserved owner name regardless of case and surrounding spaces: '#name'"() {
        when:
        Role.requireNotReservedName(name)

        then:
        thrown(DomainException)

        where:
        name << ["OWNER", "owner", "Owner", " OWNER ", "\towner"]
    }

    def "accepts names that are not the reserved owner name: '#name'"() {
        when:
        Role.requireNotReservedName(name)

        then:
        noExceptionThrown()

        where:
        name << ["MANAGER", "OWNERS", "Co-owner", "", null]
    }

    def "a custom role cannot be renamed to the owner name"() {
        given:
        Role role = buildRole("MANAGER")

        when:
        role.update("owner", "new description")

        then:
        thrown(DomainException)
        role.getName() == "MANAGER"
        role.getDescription() == "description"
    }

    def "a custom role can be renamed to another name"() {
        given:
        Role role = buildRole("MANAGER")

        when:
        role.update("FINANCE", "new description")

        then:
        role.getName() == "FINANCE"
        role.getDescription() == "new description"
        role.getUpdatedAt() != null
    }

    def "the owner role cannot be renamed"() {
        given:
        Role role = buildRole(Role.OWNER_ROLE_NAME)

        when:
        role.update(newName, "new description")

        then:
        thrown(DomainException)
        role.getName() == Role.OWNER_ROLE_NAME
        role.isOwnerRole()

        where:
        newName << ["ADMIN", "owner", "", null]
    }

    def "the owner role keeps its name and accepts a new description"() {
        given:
        Role role = buildRole(Role.OWNER_ROLE_NAME)

        when:
        role.update(Role.OWNER_ROLE_NAME, "Dono do espaço")

        then:
        role.getName() == Role.OWNER_ROLE_NAME
        role.getDescription() == "Dono do espaço"
    }
}
