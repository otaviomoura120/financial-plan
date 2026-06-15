package com.devhouse.financial_plan.application.endpointpermission

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse
import com.devhouse.financial_plan.application.endpointpermission.dto.UpdateEndpointPermissionRequest
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import spock.lang.Specification

import java.time.Instant

class UpdateEndpointPermissionServiceSpec extends Specification {

    EndpointPermissionRepository endpointPermissionRepository = Mock()
    UpdateEndpointPermissionService service = new UpdateEndpointPermissionService(endpointPermissionRepository)

    private EndpointPermission buildPermission(Long id, String group) {
        new EndpointPermission(id, 0, "/roles.*", "Old Name", null, 1,
                EndpointPermissionType.API, "GET", group, Instant.now(), null)
    }

    def "updates endpoint permission including group field"() {
        given:
        EndpointPermission existing = buildPermission(1L, "OldGroup")
        EndpointPermission updated = new EndpointPermission(1L, 0, "/roles.*", "New Name", null, 2,
                EndpointPermissionType.API, "GET,POST", "Role", Instant.now(), Instant.now())
        endpointPermissionRepository.findById(1L) >> existing
        UpdateEndpointPermissionRequest request = new UpdateEndpointPermissionRequest(
                0, "/roles.*", "New Name", null, 2, EndpointPermissionType.API, "GET,POST", "Role"
        )

        when:
        EndpointPermissionResponse response = service.execute(1L, request)

        then:
        1 * endpointPermissionRepository.update(_) >> updated
        response.name() == "New Name"
        response.group() == "Role"
        response.sequence() == 2
    }

    def "throws DomainException when group is blank on update"() {
        given:
        EndpointPermission existing = buildPermission(1L, "Role")
        endpointPermissionRepository.findById(1L) >> existing
        UpdateEndpointPermissionRequest request = new UpdateEndpointPermissionRequest(
                0, "/roles.*", "Name", null, 1, EndpointPermissionType.API, "GET", ""
        )

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * endpointPermissionRepository.update(_)
    }
}
