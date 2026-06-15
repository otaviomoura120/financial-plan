package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.EndpointPermissionEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaEndpointPermissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class EndpointPermissionRepositoryImpl implements EndpointPermissionRepository {

    private final JpaEndpointPermissionRepository jpaEndpointPermissionRepository;

    public EndpointPermissionRepositoryImpl(JpaEndpointPermissionRepository jpaEndpointPermissionRepository) {
        this.jpaEndpointPermissionRepository = jpaEndpointPermissionRepository;
    }

    @Override
    public EndpointPermission save(EndpointPermission permission) {
        EndpointPermissionEntityJpa entity = new EndpointPermissionEntityJpa();
        applyFields(permission, entity);
        EndpointPermissionEntityJpa saved = jpaEndpointPermissionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public EndpointPermission update(EndpointPermission permission) {
        EndpointPermissionEntityJpa entity = jpaEndpointPermissionRepository.findById(permission.getId()).orElseThrow();
        applyFields(permission, entity);
        EndpointPermissionEntityJpa updated = jpaEndpointPermissionRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public EndpointPermission findById(Long id) {
        return jpaEndpointPermissionRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<EndpointPermission> findAll() {
        return jpaEndpointPermissionRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<EndpointPermission> findByType(EndpointPermissionType type) {
        return jpaEndpointPermissionRepository.findByTypeOrderBySequenceAsc(type.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<EndpointPermission> findAllOrderedBySequence() {
        return jpaEndpointPermissionRepository.findAllByOrderBySequenceAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<EndpointPermission> findByGroup(String group) {
        return jpaEndpointPermissionRepository.findByEpGroupOrderBySequenceAsc(group).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaEndpointPermissionRepository.deleteById(id);
    }

    private void applyFields(EndpointPermission permission, EndpointPermissionEntityJpa entity) {
        entity.setVersion(permission.getVersion());
        entity.setEndpoint(permission.getEndpoint());
        entity.setName(permission.getName());
        entity.setIcon(permission.getIcon());
        entity.setSequence(permission.getSequence());
        entity.setType(permission.getType().name());
        entity.setPermittedMethods(permission.getPermittedMethods());
        entity.setEpGroup(permission.getGroup());
        entity.setCreatedAt(permission.getCreatedAt());
        entity.setUpdatedAt(permission.getUpdatedAt());
    }

    private EndpointPermission toDomain(EndpointPermissionEntityJpa entity) {
        return new EndpointPermission(entity.getId(), entity.getVersion(), entity.getEndpoint(), entity.getName(),
                entity.getIcon(), entity.getSequence(), EndpointPermissionType.valueOf(entity.getType()),
                entity.getPermittedMethods(), entity.getEpGroup(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
