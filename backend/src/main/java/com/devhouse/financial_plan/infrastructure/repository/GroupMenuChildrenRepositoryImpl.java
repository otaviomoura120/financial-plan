package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.repository.GroupMenuChildrenRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.GroupMenuChildrenEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaGroupMenuChildrenRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaGroupMenuRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class GroupMenuChildrenRepositoryImpl implements GroupMenuChildrenRepository {

    private final JpaGroupMenuChildrenRepository jpaGroupMenuChildrenRepository;
    private final JpaGroupMenuRepository jpaGroupMenuRepository;

    public GroupMenuChildrenRepositoryImpl(JpaGroupMenuChildrenRepository jpaGroupMenuChildrenRepository,
                                           JpaGroupMenuRepository jpaGroupMenuRepository) {
        this.jpaGroupMenuChildrenRepository = jpaGroupMenuChildrenRepository;
        this.jpaGroupMenuRepository = jpaGroupMenuRepository;
    }

    @Override
    public GroupMenuChildren save(GroupMenuChildren child) {
        GroupMenuChildrenEntityJpa entity = new GroupMenuChildrenEntityJpa();
        entity.setName(child.getName());
        entity.setEndpoint(child.getEndpoint());
        entity.setIcon(child.getIcon());
        entity.setGroupMenu(jpaGroupMenuRepository.getReferenceById(child.getGroupMenu().getId()));
        entity.setCreatedAt(child.getCreatedAt());
        entity.setUpdatedAt(child.getUpdatedAt());
        GroupMenuChildrenEntityJpa saved = jpaGroupMenuChildrenRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public GroupMenuChildren update(GroupMenuChildren child) {
        GroupMenuChildrenEntityJpa entity = jpaGroupMenuChildrenRepository.findById(child.getId()).orElseThrow();
        entity.setName(child.getName());
        entity.setEndpoint(child.getEndpoint());
        entity.setIcon(child.getIcon());
        entity.setUpdatedAt(child.getUpdatedAt());
        GroupMenuChildrenEntityJpa updated = jpaGroupMenuChildrenRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public GroupMenuChildren findById(Long id) {
        return jpaGroupMenuChildrenRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<GroupMenuChildren> findByGroupMenuId(Long groupMenuId) {
        return jpaGroupMenuChildrenRepository.findByGroupMenuId(groupMenuId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaGroupMenuChildrenRepository.deleteById(id);
    }

    private GroupMenuChildren toDomain(GroupMenuChildrenEntityJpa entity) {
        GroupMenu groupMenu = new GroupMenu(entity.getGroupMenu().getId(), entity.getGroupMenu().getVersion(),
                entity.getGroupMenu().getName(), entity.getGroupMenu().getIcon(), List.of(),
                entity.getGroupMenu().getCreatedAt(), entity.getGroupMenu().getUpdatedAt());
        return new GroupMenuChildren(entity.getId(), entity.getVersion(), entity.getName(), entity.getEndpoint(),
                entity.getIcon(), groupMenu, entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
