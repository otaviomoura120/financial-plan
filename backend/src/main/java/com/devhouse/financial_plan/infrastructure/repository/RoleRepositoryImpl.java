package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaFamilyRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;
    private final JpaFamilyRepository jpaFamilyRepository;

    public RoleRepositoryImpl(JpaRoleRepository jpaRoleRepository, JpaFamilyRepository jpaFamilyRepository) {
        this.jpaRoleRepository = jpaRoleRepository;
        this.jpaFamilyRepository = jpaFamilyRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntityJpa entity = new RoleEntityJpa();
        entity.setFamily(jpaFamilyRepository.getReferenceById(role.getFamily().getId()));
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
    public List<Role> findByFamilyId(Long familyId) {
        return jpaRoleRepository.findByFamilyId(familyId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaRoleRepository.deleteById(id);
    }

    private Role toDomain(RoleEntityJpa entity) {
        Family family = new Family(entity.getFamily().getId(), null, entity.getFamily().getName(), entity.getFamily().getCreatedAt(), null);
        return new Role(entity.getId(), entity.getVersion(), family, entity.getName(), entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
