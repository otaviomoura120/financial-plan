package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class SubCategoryRepositoryImpl implements SubCategoryRepository {

    private final JpaSubCategoryRepository jpaSubCategoryRepository;
    private final JpaCategoryRepository jpaCategoryRepository;

    public SubCategoryRepositoryImpl(JpaSubCategoryRepository jpaSubCategoryRepository, JpaCategoryRepository jpaCategoryRepository) {
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
    }

    @Override
    public SubCategory save(SubCategory subCategory) {
        SubCategoryEntityJpa entity = new SubCategoryEntityJpa();
        applyFields(subCategory, entity);
        SubCategoryEntityJpa saved = jpaSubCategoryRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public SubCategory update(SubCategory subCategory) {
        SubCategoryEntityJpa entity = jpaSubCategoryRepository.findById(subCategory.getId()).orElseThrow();
        applyFields(subCategory, entity);
        SubCategoryEntityJpa updated = jpaSubCategoryRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public SubCategory findById(Long id) {
        return jpaSubCategoryRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<SubCategory> findByCategoryId(Long categoryId) {
        return jpaSubCategoryRepository.findByCategoryId(categoryId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaSubCategoryRepository.deleteById(id);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return jpaSubCategoryRepository.existsByCategoryId(categoryId);
    }

    private void applyFields(SubCategory subCategory, SubCategoryEntityJpa entity) {
        entity.setCategory(jpaCategoryRepository.getReferenceById(subCategory.getCategory().getId()));
        entity.setName(subCategory.getName());
        entity.setActive(subCategory.isActive());
    }

    private SubCategory toDomain(SubCategoryEntityJpa entity) {
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        return new SubCategory(entity.getId(), entity.getVersion(), category, entity.getName(),
                entity.isActive(), null, null);
    }

    private Category buildCategory(CategoryEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new Category(entity.getId(), entity.getVersion(), space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
