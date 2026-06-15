package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.RoleEndpointPermissionResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.RoleEndpointPermission
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class GetRolePermissionsServiceSpec extends Specification {

    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    UserRepository userRepository = Mock()
    GetRolePermissionsService service = new GetRolePermissionsService(roleEndpointPermissionRepository, userRepository)

    private User buildUser(boolean masterAdmin) {
        new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, masterAdmin)
    }

    private RoleEndpointPermission buildRelation(Long id, String group) {
        Space space = new Space(1L, 0, "Space", null, Instant.now(), null)
        Role role = new Role(1L, 0, space, "ADMIN", null, Instant.now(), null)
        EndpointPermission ep = new EndpointPermission(id, 0, "/path.*", "Name", null, 1,
                EndpointPermissionType.API, "GET", group, Instant.now(), null)
        new RoleEndpointPermission(id * 10, 0, role, ep, EndpointPermissionAccess.DENY, Instant.now(), null)
    }

    def "filters out internal_management permissions when caller is not MASTER_ADMIN"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        roleEndpointPermissionRepository.findByRoleId(1L) >> [
                buildRelation(1L, "Role"),
                buildRelation(2L, EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        List<RoleEndpointPermissionResponse> result = service.execute(1L, "auth0|abc")

        then:
        result.size() == 1
        result[0].group() == "Role"
    }

    def "includes internal_management permissions when caller is MASTER_ADMIN"() {
        given:
        userRepository.findByAuth0Sub("auth0|master") >> buildUser(true)
        roleEndpointPermissionRepository.findByRoleId(1L) >> [
                buildRelation(1L, "Role"),
                buildRelation(2L, EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        List<RoleEndpointPermissionResponse> result = service.execute(1L, "auth0|master")

        then:
        result.size() == 2
    }

    def "returns empty list when role has no permissions"() {
        given:
        userRepository.findByAuth0Sub("auth0|abc") >> buildUser(false)
        roleEndpointPermissionRepository.findByRoleId(1L) >> []

        when:
        List<RoleEndpointPermissionResponse> result = service.execute(1L, "auth0|abc")

        then:
        result.isEmpty()
    }

    def "filters internal_management when caller user is not found"() {
        given:
        userRepository.findByAuth0Sub("auth0|unknown") >> null
        roleEndpointPermissionRepository.findByRoleId(1L) >> [
                buildRelation(1L, "Role"),
                buildRelation(2L, EndpointPermission.INTERNAL_MANAGEMENT_GROUP)
        ]

        when:
        List<RoleEndpointPermissionResponse> result = service.execute(1L, "auth0|unknown")

        then:
        result.size() == 1
        result[0].group() == "Role"
    }
}
