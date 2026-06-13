package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.GroupMenuEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaGroupMenuRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class GroupMenuRepositoryImpl implements GroupMenuRepository {

    private final JpaGroupMenuRepository jpaGroupMenuRepository;

    public GroupMenuRepositoryImpl(JpaGroupMenuRepository jpaGroupMenuRepository) {
        this.jpaGroupMenuRepository = jpaGroupMenuRepository;
    }

    @Override
    public GroupMenu save(GroupMenu groupMenu) {
        GroupMenuEntityJpa entity = new GroupMenuEntityJpa();
        entity.setName(groupMenu.getName());
        entity.setIcon(groupMenu.getIcon());
        entity.setCreatedAt(groupMenu.getCreatedAt());
        entity.setUpdatedAt(groupMenu.getUpdatedAt());
        GroupMenuEntityJpa saved = jpaGroupMenuRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public GroupMenu update(GroupMenu groupMenu) {
        GroupMenuEntityJpa entity = jpaGroupMenuRepository.findById(groupMenu.getId()).orElseThrow();
        entity.setName(groupMenu.getName());
        entity.setIcon(groupMenu.getIcon());
        entity.setUpdatedAt(groupMenu.getUpdatedAt());
        GroupMenuEntityJpa updated = jpaGroupMenuRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public GroupMenu findById(Long id) {
        return jpaGroupMenuRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<GroupMenu> findAllWithChildren() {
        return jpaGroupMenuRepository.findAll().stream()
                .map(this::toDomainWithChildren)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaGroupMenuRepository.deleteById(id);
    }

    private GroupMenu toDomain(GroupMenuEntityJpa entity) {
        return new GroupMenu(entity.getId(), entity.getName(), entity.getIcon(), List.of(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private GroupMenu toDomainWithChildren(GroupMenuEntityJpa entity) {
        GroupMenu groupMenu = new GroupMenu(entity.getId(), entity.getName(), entity.getIcon(), List.of(), entity.getCreatedAt(), entity.getUpdatedAt());
        if (entity.getChildren() != null) {
            List<GroupMenuChildren> children = entity.getChildren().stream()
                    .map(child -> new GroupMenuChildren(child.getId(), child.getName(), child.getEndpoint(), child.getIcon(), groupMenu, child.getCreatedAt(), child.getUpdatedAt()))
                    .toList();
            groupMenu.setChildren(children);
        }
        return groupMenu;
    }
}
