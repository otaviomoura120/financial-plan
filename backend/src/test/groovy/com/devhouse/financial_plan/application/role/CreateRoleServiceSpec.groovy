package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest
import com.devhouse.financial_plan.application.role.dto.RoleResponse
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreateRoleServiceSpec extends Specification {

    RoleRepository roleRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CreateRoleService service = new CreateRoleService(roleRepository, spaceRepository)

    def "execute creates role with space and returns response"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "ADMIN", "Administrator role")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        Role savedRole = new Role(10L, 0, space, "ADMIN", "Administrator role", Instant.now(), null)
        roleRepository.save(_) >> savedRole

        when:
        RoleResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.name() == "ADMIN"
        response.description() == "Administrator role"
        response.spaceId() == 1L
    }

    def "execute throws DomainException when role name is blank"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "", "description")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }

    def "execute throws DomainException when space is null"() {
        given:
        CreateRoleRequest request = new CreateRoleRequest(1L, "ADMIN", "description")
        spaceRepository.findById(1L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }
}
