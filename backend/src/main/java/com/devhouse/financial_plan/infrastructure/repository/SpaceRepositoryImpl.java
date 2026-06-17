package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceMemberRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class SpaceRepositoryImpl implements SpaceRepository {

    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaSpaceMemberRepository jpaSpaceMemberRepository;

    public SpaceRepositoryImpl(JpaSpaceRepository jpaSpaceRepository, JpaSpaceMemberRepository jpaSpaceMemberRepository) {
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaSpaceMemberRepository = jpaSpaceMemberRepository;
    }

    @Override
    public Space save(Space space) {
        SpaceEntityJpa entity = new SpaceEntityJpa();
        applyFields(space, entity);
        SpaceEntityJpa saved = jpaSpaceRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Space update(Space space) {
        SpaceEntityJpa entity = jpaSpaceRepository.findById(space.getId()).orElseThrow();
        applyFields(space, entity);
        SpaceEntityJpa updated = jpaSpaceRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public Space findById(Long id) {
        return jpaSpaceRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Space> findByUserId(Long userId) {
        return jpaSpaceMemberRepository.findByUserId(userId).stream()
                .map(member -> toDomain(member.getSpace()))
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaSpaceRepository.deleteById(id);
    }

    private void applyFields(Space space, SpaceEntityJpa entity) {
        entity.setVersion(space.getVersion());
        entity.setName(space.getName());
        entity.setDescription(space.getDescription());
        entity.setCreatedAt(space.getCreatedDate());
        entity.setUpdatedAt(space.getUpdatedDate());
    }

    private Space toDomain(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
