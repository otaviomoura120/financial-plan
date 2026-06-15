package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JpaRoleEndpointPermissionRepository extends JpaRepository<RoleEndpointPermissionEntityJpa, Long> {

    @Query("SELECT rep FROM RoleEndpointPermissionEntityJpa rep " +
           "JOIN FETCH rep.endpointPermission " +
           "JOIN FETCH rep.role r " +
           "JOIN FETCH r.space " +
           "WHERE rep.role.id = :roleId " +
           "ORDER BY rep.endpointPermission.sequence ASC")
    List<RoleEndpointPermissionEntityJpa> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT rep.endpointPermission FROM RoleEndpointPermissionEntityJpa rep " +
           "WHERE rep.role.id IN :roleIds " +
           "AND rep.permission = 'ALLOW' " +
           "AND rep.endpointPermission.type = :type")
    List<EndpointPermissionEntityJpa> findAllowedEndpointPermissionsByRoleIdsAndType(
            @Param("roleIds") Set<Long> roleIds,
            @Param("type") String type);

    Optional<RoleEndpointPermissionEntityJpa> findByRoleIdAndEndpointPermissionId(Long roleId, Long endpointPermissionId);

    @Modifying
    void deleteByRoleId(Long roleId);

    @Modifying
    void deleteByEndpointPermissionId(Long endpointPermissionId);
}
