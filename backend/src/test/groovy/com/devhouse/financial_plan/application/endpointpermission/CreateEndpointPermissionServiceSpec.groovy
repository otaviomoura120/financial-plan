package com.devhouse.financial_plan.application.endpointpermission

import com.devhouse.financial_plan.application.endpointpermission.dto.CreateEndpointPermissionRequest
import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse
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
import spock.lang.Specification

import java.time.Instant

class CreateEndpointPermissionServiceSpec extends Specification {

    EndpointPermissionRepository endpointPermissionRepository = Mock()
    RoleRepository roleRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    CreateEndpointPermissionService service = new CreateEndpointPermissionService(
            endpointPermissionRepository, roleRepository, roleEndpointPermissionRepository)

    private Role buildRole(Long id) {
        Space space = new Space(1L, 0, "Space", null, Instant.now(), null)
        new Role(id, 0, space, "ADMIN", null, Instant.now(), null)
    }

    private EndpointPermission buildSavedPermission() {
        new EndpointPermission(5L, 0, "/roles.*", "Roles Endpoint", null,
                1, EndpointPermissionType.API, "GET,POST", "Role", Instant.now(), null)
    }

    def "creates endpoint permission and saves DENY relations for all existing roles"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Roles Endpoint", null, 1, EndpointPermissionType.API, "GET,POST", "Role"
        )
        endpointPermissionRepository.save(_) >> buildSavedPermission()
        roleRepository.findAll() >> [buildRole(1L), buildRole(2L)]

        when:
        EndpointPermissionResponse response = service.execute(request)

        then:
        response.id() == 5L
        response.endpoint() == "/roles.*"
        response.permittedMethods() == "GET,POST"
        response.type() == EndpointPermissionType.API
        response.group() == "Role"
        1 * roleEndpointPermissionRepository.saveAll({ List<RoleEndpointPermission> relations ->
            relations.size() == 2 &&
            relations.every { it.getPermission() == EndpointPermissionAccess.DENY } &&
            relations.every { it.getEndpointPermission().getId() == 5L }
        })
    }

    def "creates endpoint permission with no role relations when no roles exist"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Roles Endpoint", null, 1, EndpointPermissionType.API, "GET", "Role"
        )
        endpointPermissionRepository.save(_) >> buildSavedPermission()
        roleRepository.findAll() >> []

        when:
        service.execute(request)

        then:
        1 * roleEndpointPermissionRepository.saveAll([])
    }

    def "throws DomainException when endpoint is blank"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "", "Name", null, 1, EndpointPermissionType.API, "GET", "Role"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }

    def "throws DomainException when name is blank"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "", null, 1, EndpointPermissionType.API, "GET", "Role"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }

    def "throws DomainException when type is null"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Name", null, 1, null, "GET", "Role"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }

    def "throws DomainException when group is blank"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Name", null, 1, EndpointPermissionType.API, "GET", ""
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }
}
