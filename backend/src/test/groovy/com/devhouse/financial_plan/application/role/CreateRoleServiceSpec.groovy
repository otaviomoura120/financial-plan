package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest
import com.devhouse.financial_plan.application.role.dto.RoleResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.RoleEndpointPermission
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreateRoleServiceSpec extends Specification {

    RoleRepository roleRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    CreateRoleService service = new CreateRoleService(roleRepository, spaceRepository, endpointPermissionRepository, roleEndpointPermissionRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private EndpointPermission buildEndpointPermission(Long id) {
        new EndpointPermission(id, 0, "/path.*", "Name", null, 1, EndpointPermissionType.API, "GET", Instant.now(), null)
    }

    def "creates role and saves DENY relations for all existing endpoint permissions"() {
        given:
        Space space = buildSpace()
        Role savedRole = new Role(10L, 0, space, "MANAGER", "Gerente", Instant.now(), null)
        endpointPermissionRepository.findAll() >> [buildEndpointPermission(1L), buildEndpointPermission(2L)]
        spaceRepository.findById(1L) >> space
        roleRepository.save(_) >> savedRole

        when:
        RoleResponse result = service.execute(new CreateRoleRequest(1L, "MANAGER", "Gerente"))

        then:
        result.id() == 10L
        result.name() == "MANAGER"
        1 * roleEndpointPermissionRepository.saveAll({ List<RoleEndpointPermission> relations ->
            relations.size() == 2 &&
            relations.every { it.getPermission() == EndpointPermissionAccess.DENY } &&
            relations.every { it.getRole().getId() == 10L }
        })
    }

    def "creates role with empty permission list when no endpoint permissions exist"() {
        given:
        Space space = buildSpace()
        Role savedRole = new Role(10L, 0, space, "MANAGER", "Gerente", Instant.now(), null)
        endpointPermissionRepository.findAll() >> []
        spaceRepository.findById(1L) >> space
        roleRepository.save(_) >> savedRole

        when:
        service.execute(new CreateRoleRequest(1L, "MANAGER", "Gerente"))

        then:
        1 * roleEndpointPermissionRepository.saveAll([])
    }

    def "throws DomainException when role name is blank"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()

        when:
        service.execute(new CreateRoleRequest(1L, "", "description"))

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }

    def "throws DomainException when space is null"() {
        given:
        spaceRepository.findById(1L) >> null

        when:
        service.execute(new CreateRoleRequest(1L, "ADMIN", "description"))

        then:
        thrown(DomainException)
        0 * roleRepository.save(_)
    }
}
