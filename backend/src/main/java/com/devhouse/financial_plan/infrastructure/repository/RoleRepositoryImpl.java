package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;
    private final JpaSpaceRepository jpaSpaceRepository;

    public RoleRepositoryImpl(JpaRoleRepository jpaRoleRepository, JpaSpaceRepository jpaSpaceRepository) {
        this.jpaRoleRepository = jpaRoleRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntityJpa entity = new RoleEntityJpa();
        entity.setSpace(jpaSpaceRepository.getReferenceById(role.getSpace().getId()));
        entity.setVersion(role.getVersion());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setCreatedAt(role.getCreatedAt());
        entity.setUpdatedAt(role.getUpdatedAt());
        RoleEntityJpa saved = jpaRoleRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Role update(Role role) {
        RoleEntityJpa entity = jpaRoleRepository.findById(role.getId()).orElseThrow();
        entity.setVersion(role.getVersion());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setUpdatedAt(role.getUpdatedAt());
        RoleEntityJpa updated = jpaRoleRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public Role findById(Long id) {
        return jpaRoleRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Role> findAll() {
        return jpaRoleRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Role> findBySpaceId(Long spaceId) {
        return jpaRoleRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaRoleRepository.deleteById(id);
    }

    private Role toDomain(RoleEntityJpa entity) {
        Space space = buildSpace(entity.getSpace());
        return new Role(entity.getId(), entity.getVersion(), space, entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
