package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest
import com.devhouse.financial_plan.application.role.dto.RoleResponse
import com.devhouse.financial_plan.domain.Family
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.FamilyRepository
import com.devhouse.financial_plan.domain.repository.RoleRepository
import spock.lang.Specification

import java.time.Instant

class CreateRoleServiceSpec extends Specification {

    RoleRepository roleRepository = Mock()
    FamilyRepository familyRepository = Mock()
    CreateRoleService service = new CreateRoleService(roleRepository, familyRepository)

    def "execute creates role with family and returns response"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "ADMIN", "Administrator role")
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        familyRepository.findById(1L) >> family

        Role savedRole = new Role(10L, 0, family, "ADMIN", "Administrator role", Instant.now(), null)
        roleRepository.save(_) >> savedRole

        when:
        RoleResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.name() == "ADMIN"
        response.description() == "Administrator role"
        response.familyId() == 1L
    }

    def "execute throws DomainException when role name is blank"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "", "description")
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        familyRepository.findById(1L) >> family

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }

    def "execute throws DomainException when family is null"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "ADMIN", "description")
        familyRepository.findById(1L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }
}
