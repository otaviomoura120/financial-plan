package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.application.role.dto.UpdateRolePermissionAccessRequest
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.RoleEndpointPermission
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import spock.lang.Specification

import java.time.Instant

class UpdateRolePermissionAccessServiceSpec extends Specification {

    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    UpdateRolePermissionAccessService service = new UpdateRolePermissionAccessService(roleEndpointPermissionRepository)

    private RoleEndpointPermission buildRelation(EndpointPermissionAccess access) {
        Space space = new Space(1L, 0, "Space", null, Instant.now(), null)
        Role role = new Role(1L, 0, space, "ADMIN", null, Instant.now(), null)
        EndpointPermission ep = new EndpointPermission(1L, 0, "/roles.*", "Roles", null, 1,
                EndpointPermissionType.API, "GET", "Role", Instant.now(), null)
        new RoleEndpointPermission(10L, 0, role, ep, access, Instant.now(), null)
    }

    def "changes permission from DENY to ALLOW"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.DENY)
        roleEndpointPermissionRepository.findByRoleIdAndEndpointPermissionId(1L, 1L) >> relation
        UpdateRolePermissionAccessRequest request = new UpdateRolePermissionAccessRequest(0, EndpointPermissionAccess.ALLOW)

        when:
        service.execute(1L, 1L, request)

        then:
        1 * roleEndpointPermissionRepository.update({ RoleEndpointPermission r ->
            r.isAllowed() == true
        })
    }

    def "changes permission from ALLOW to DENY"() {
        given:
        RoleEndpointPermission relation = buildRelation(EndpointPermissionAccess.ALLOW)
        roleEndpointPermissionRepository.findByRoleIdAndEndpointPermissionId(1L, 1L) >> relation
        UpdateRolePermissionAccessRequest request = new UpdateRolePermissionAccessRequest(0, EndpointPermissionAccess.DENY)

        when:
        service.execute(1L, 1L, request)

        then:
        1 * roleEndpointPermissionRepository.update({ RoleEndpointPermission r ->
            r.isAllowed() == false
        })
    }

    def "throws DomainException when relation not found"() {
        given:
        roleEndpointPermissionRepository.findByRoleIdAndEndpointPermissionId(1L, 99L) >> null

        when:
        service.execute(1L, 99L, new UpdateRolePermissionAccessRequest(0, EndpointPermissionAccess.ALLOW))

        then:
        thrown(DomainException)
        0 * roleEndpointPermissionRepository.update(_)
    }
}
