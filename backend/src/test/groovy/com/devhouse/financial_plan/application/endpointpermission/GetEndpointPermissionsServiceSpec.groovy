package com.devhouse.financial_plan.application.endpointpermission

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import spock.lang.Specification

import java.time.Instant

class GetEndpointPermissionsServiceSpec extends Specification {

    EndpointPermissionRepository endpointPermissionRepository = Mock()
    GetEndpointPermissionsService service = new GetEndpointPermissionsService(endpointPermissionRepository)

    private EndpointPermission buildPermission(Long id, String group) {
        new EndpointPermission(id, 0, "/roles.*", "Roles", null, 1,
                EndpointPermissionType.API, "GET", group, Instant.now(), null)
    }

    def "returns all permissions ordered by sequence when group is null"() {
        given:
        endpointPermissionRepository.findAllOrderedBySequence() >> [buildPermission(1L, "Role"), buildPermission(2L, "User")]

        when:
        List<EndpointPermissionResponse> result = service.execute(null)

        then:
        result.size() == 2
        0 * endpointPermissionRepository.findByGroup(_)
    }

    def "returns all permissions ordered by sequence when group is blank"() {
        given:
        endpointPermissionRepository.findAllOrderedBySequence() >> [buildPermission(1L, "Role")]

        when:
        List<EndpointPermissionResponse> result = service.execute("  ")

        then:
        result.size() == 1
        0 * endpointPermissionRepository.findByGroup(_)
    }

    def "returns permissions filtered by group when group is provided"() {
        given:
        endpointPermissionRepository.findByGroup("Role") >> [buildPermission(1L, "Role")]

        when:
        List<EndpointPermissionResponse> result = service.execute("Role")

        then:
        result.size() == 1
        result[0].group() == "Role"
        0 * endpointPermissionRepository.findAllOrderedBySequence()
    }

    def "returns empty list when no permissions match the group"() {
        given:
        endpointPermissionRepository.findByGroup("Unknown") >> []

        when:
        List<EndpointPermissionResponse> result = service.execute("Unknown")

        then:
        result.isEmpty()
    }
}
