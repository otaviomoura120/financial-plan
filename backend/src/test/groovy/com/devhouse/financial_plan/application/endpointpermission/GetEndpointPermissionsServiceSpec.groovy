package com.devhouse.financial_plan.application.endpointpermission

import com.devhouse.financial_plan.application.endpointpermission.dto.EndpointPermissionResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class GetEndpointPermissionsServiceSpec extends Specification {

    EndpointPermissionRepository endpointPermissionRepository = Mock()
    UserRepository userRepository = Mock()
    GetEndpointPermissionsService service = new GetEndpointPermissionsService(endpointPermissionRepository, userRepository)

    private User buildUser(boolean masterAdmin) {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, masterAdmin)
    }

    private EndpointPermission buildPermission(Long id, String group) {
        new EndpointPermission(id, 0, "/roles.*", "Roles", null, 1,
                EndpointPermissionType.API, "GET", group, Instant.now(), null)
    }

    def "hides internal_management permissions when caller is not MASTER_ADMIN"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        endpointPermissionRepository.findAllOrderedBySequence() >> [
                buildPermission(1L, "Role"),
                buildPermission(2L, EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        List<EndpointPermissionResponse> result = service.execute(null, "auth0|abc")

        then:
        result.size() == 1
        result[0].group() == "Role"
        0 * endpointPermissionRepository.findByGroup(_)
    }

    def "shows internal_management permissions when caller is MASTER_ADMIN"() {
        given:
        userRepository.findByAuth0Sub("auth0|master") >> buildUser(true)
        endpointPermissionRepository.findAllOrderedBySequence() >> [
                buildPermission(1L, "Role"),
                buildPermission(2L, EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        List<EndpointPermissionResponse> result = service.execute(null, "auth0|master")

        then:
        result.size() == 2
    }

    def "hides internal_management when group filter is blank and caller is not MASTER_ADMIN"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        endpointPermissionRepository.findAllOrderedBySequence() >> [buildPermission(1L, "Role")]

        when:
        List<EndpointPermissionResponse> result = service.execute("  ", "auth0|abc")

        then:
        result.size() == 1
        0 * endpointPermissionRepository.findByGroup(_)
    }

    def "returns permissions filtered by group when group is provided"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        endpointPermissionRepository.findByGroup("Role") >> [buildPermission(1L, "Role")]

        when:
        List<EndpointPermissionResponse> result = service.execute("Role", "auth0|abc")

        then:
        result.size() == 1
        result[0].group() == "Role"
        0 * endpointPermissionRepository.findAllOrderedBySequence()
    }

    def "returns empty list when no permissions match the group"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        endpointPermissionRepository.findByGroup("Unknown") >> []

        when:
        List<EndpointPermissionResponse> result = service.execute("Unknown", "auth0|abc")

        then:
        result.isEmpty()
    }
}
