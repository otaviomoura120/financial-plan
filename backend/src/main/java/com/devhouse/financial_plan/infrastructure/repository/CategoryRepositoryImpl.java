package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class CategoryRepositoryImpl implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSpaceRepository jpaSpaceRepository;

    public CategoryRepositoryImpl(JpaCategoryRepository jpaCategoryRepository, JpaSpaceRepository jpaSpaceRepository) {
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryEntityJpa entity = new CategoryEntityJpa();
        applyFields(category, entity);
        CategoryEntityJpa saved = jpaCategoryRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Category update(Category category) {
        CategoryEntityJpa entity = jpaCategoryRepository.findById(category.getId()).orElseThrow();
        entity.setName(category.getName());
        entity.setActive(category.isActive());
        entity.setUpdatedAt(category.getUpdatedDate());
        CategoryEntityJpa updated = jpaCategoryRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public Category findById(Long id) {
        return jpaCategoryRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Category> findBySpaceId(Long spaceId) {
        return jpaCategoryRepository.findBySpaceIdOrderByNameAsc(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaCategoryRepository.deleteById(id);
    }

    private void applyFields(Category category, CategoryEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(category.getSpace().getId()));
        entity.setName(category.getName());
        entity.setActive(category.isActive());
        entity.setCreatedAt(category.getCreatedDate());
        entity.setUpdatedAt(category.getUpdatedDate());
    }

    private Category toDomain(CategoryEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new Category(entity.getId(), entity.getVersion(), space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
