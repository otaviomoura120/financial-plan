package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.EndpointPermissionEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaEndpointPermissionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleEndpointPermissionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEndpointPermissionEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@Transactional
public class RoleEndpointPermissionRepositoryImpl implements RoleEndpointPermissionRepository {

    private final JpaRoleEndpointPermissionRepository jpaRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final JpaEndpointPermissionRepository jpaEndpointPermissionRepository;

    public RoleEndpointPermissionRepositoryImpl(JpaRoleEndpointPermissionRepository jpaRepository,
                                                JpaRoleRepository jpaRoleRepository,
                                                JpaEndpointPermissionRepository jpaEndpointPermissionRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaRoleRepository = jpaRoleRepository;
        this.jpaEndpointPermissionRepository = jpaEndpointPermissionRepository;
    }

    @Override
    public void saveAll(List<RoleEndpointPermission> relations) {
        List<RoleEndpointPermissionEntityJpa> entities = relations.stream()
                .map(this::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public RoleEndpointPermission findByRoleIdAndEndpointPermissionId(Long roleId, Long endpointPermissionId) {
        return jpaRepository.findByRoleIdAndEndpointPermissionId(roleId, endpointPermissionId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<RoleEndpointPermission> findByRoleId(Long roleId) {
        return jpaRepository.findByRoleId(roleId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<EndpointPermission> findAllowedEndpointPermissionsByRoleIdsAndType(Set<Long> roleIds, EndpointPermissionType type) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllowedEndpointPermissionsByRoleIdsAndType(roleIds, type.name()).stream()
                .map(this::toEndpointPermissionDomain)
                .toList();
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        jpaRepository.deleteByRoleId(roleId);
    }

    @Override
    public void deleteByEndpointPermissionId(Long endpointPermissionId) {
        jpaRepository.deleteByEndpointPermissionId(endpointPermissionId);
    }

    @Override
    public RoleEndpointPermission update(RoleEndpointPermission relation) {
        RoleEndpointPermissionEntityJpa entity = jpaRepository.findById(relation.getId()).orElseThrow();
        entity.setVersion(relation.getVersion());
        entity.setPermission(relation.getPermission().name());
        entity.setUpdatedAt(relation.getUpdatedAt());
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    private RoleEndpointPermissionEntityJpa toEntity(RoleEndpointPermission relation) {
        RoleEndpointPermissionEntityJpa entity = new RoleEndpointPermissionEntityJpa();
        entity.setRole(jpaRoleRepository.getReferenceById(relation.getRole().getId()));
        entity.setEndpointPermission(jpaEndpointPermissionRepository.getReferenceById(relation.getEndpointPermission().getId()));
        entity.setVersion(relation.getVersion());
        entity.setPermission(relation.getPermission().name());
        entity.setCreatedAt(relation.getCreatedAt());
        entity.setUpdatedAt(relation.getUpdatedAt());
        return entity;
    }

    private RoleEndpointPermission toDomain(RoleEndpointPermissionEntityJpa entity) {
        Role role = buildRole(entity.getRole());
        EndpointPermission ep = toEndpointPermissionDomain(entity.getEndpointPermission());
        EndpointPermissionAccess access = EndpointPermissionAccess.valueOf(entity.getPermission());
        return new RoleEndpointPermission(entity.getId(), entity.getVersion(), role, ep, access,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Role buildRole(RoleEntityJpa entity) {
        Space space = buildSpace(entity.getSpace());
        return new Role(entity.getId(), entity.getVersion(), space, entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private EndpointPermission toEndpointPermissionDomain(EndpointPermissionEntityJpa entity) {
        return new EndpointPermission(entity.getId(), entity.getVersion(), entity.getEndpoint(), entity.getName(),
                entity.getIcon(), entity.getSequence(), EndpointPermissionType.valueOf(entity.getType()),
                entity.getPermittedMethods(), entity.getEpGroup(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
