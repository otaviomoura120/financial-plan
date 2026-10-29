package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.RoleResponse
import com.devhouse.financial_plan.application.role.dto.UpdateRoleRequest
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.RoleRepository
import spock.lang.Specification

import java.time.Instant

class UpdateRoleServiceSpec extends Specification {

    RoleRepository roleRepository = Mock()
    UpdateRoleService service = new UpdateRoleService(roleRepository)

    private Role buildRole(String name) {
        new Role(10L, 0, new Space(1L, 0, "My Space", null, Instant.now(), null), name, "description", Instant.now(), null)
    }

    def "renames a custom role and persists it"() {
        given:
        roleRepository.findById(10L) >> buildRole("MANAGER")
        roleRepository.update(_ as Role) >> { Role role -> role }

        when:
        RoleResponse result = service.execute(10L, new UpdateRoleRequest(0, "FINANCE", "Financeiro"))

        then:
        result.name() == "FINANCE"
        result.description() == "Financeiro"
    }

    def "does not persist a custom role renamed to the owner name"() {
        given:
        roleRepository.findById(10L) >> buildRole("MANAGER")

        when:
        service.execute(10L, new UpdateRoleRequest(0, "Owner", "description"))

        then:
        thrown(DomainException)
        0 * roleRepository.update(_)
    }

    def "does not persist a rename of the owner role"() {
        given:
        roleRepository.findById(10L) >> buildRole(Role.OWNER_ROLE_NAME)

        when:
        service.execute(10L, new UpdateRoleRequest(0, "ADMIN", "description"))

        then:
        thrown(DomainException)
        0 * roleRepository.update(_)
    }

    def "updates the owner role description when the name is kept"() {
        given:
        roleRepository.findById(10L) >> buildRole(Role.OWNER_ROLE_NAME)
        roleRepository.update(_ as Role) >> { Role role -> role }

        when:
        RoleResponse result = service.execute(10L, new UpdateRoleRequest(0, Role.OWNER_ROLE_NAME, "Dono"))

        then:
        result.name() == Role.OWNER_ROLE_NAME
        result.description() == "Dono"
    }
}
