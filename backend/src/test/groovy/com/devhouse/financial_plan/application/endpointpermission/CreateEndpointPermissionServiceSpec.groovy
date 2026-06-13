package com.devhouse.financial_plan.application.endpointpermission

import com.devhouse.financial_plan.application.endpointpermission.dto.CreateEndpointPermissionRequest
import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import spock.lang.Specification

import java.time.Instant

class CreateEndpointPermissionServiceSpec extends Specification {

    EndpointPermissionRepository endpointPermissionRepository = Mock()
    CreateEndpointPermissionService service = new CreateEndpointPermissionService(endpointPermissionRepository)

    def "execute creates endpoint permission and returns response"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Roles Endpoint", null, 1, EndpointPermissionType.API, "GET,POST", "ADMIN"
        )
        EndpointPermission saved = new EndpointPermission(5L, 0, "/roles.*", "Roles Endpoint", null,
                1, EndpointPermissionType.API, "GET,POST", "ADMIN", Instant.now(), null)
        endpointPermissionRepository.save(_) >> saved

        when:
        EndpointPermissionResponse response = service.execute(request)

        then:
        response.id() == 5L
        response.endpoint() == "/roles.*"
        response.permittedMethods() == "GET,POST"
        response.permittedRoles() == "ADMIN"
        response.type() == EndpointPermissionType.API
    }

    def "execute throws DomainException when endpoint is blank"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "", "Name", null, 1, EndpointPermissionType.API, "GET", "ADMIN"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }

    def "execute throws DomainException when name is blank"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "", null, 1, EndpointPermissionType.API, "GET", "ADMIN"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }

    def "execute throws DomainException when type is null"() {
        given:
        CreateEndpointPermissionRequest request = new CreateEndpointPermissionRequest(
                "/roles.*", "Name", null, 1, null, "GET", "ADMIN"
        )

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.save(_)
    }
}
