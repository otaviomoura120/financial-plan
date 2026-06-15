package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

import java.util.List;
import java.util.Set;

public interface RoleEndpointPermissionRepository {

    void saveAll(List<RoleEndpointPermission> relations);
    RoleEndpointPermission findByRoleIdAndEndpointPermissionId(Long roleId, Long endpointPermissionId);
    List<RoleEndpointPermission> findByRoleId(Long roleId);
    List<EndpointPermission> findAllowedEndpointPermissionsByRoleIdsAndType(Set<Long> roleIds, EndpointPermissionType type);
    void deleteByRoleId(Long roleId);
    void deleteByEndpointPermissionId(Long endpointPermissionId);
    RoleEndpointPermission update(RoleEndpointPermission relation);
}
