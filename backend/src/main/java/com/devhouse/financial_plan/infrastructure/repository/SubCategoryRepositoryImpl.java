package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class SubCategoryRepositoryImpl implements SubCategoryRepository {

    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    public SubCategoryRepositoryImpl(JpaSubCategoryRepository jpaSubCategoryRepository) {
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
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
        entity.setCategoryId(subCategory.getCategoryId());
        entity.setName(subCategory.getName());
        entity.setActive(subCategory.isActive());
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

    private void applyFields(SubCategory subCategory, SubCategoryEntityJpa entity) {
        entity.setCategoryId(subCategory.getCategoryId());
        entity.setName(subCategory.getName());
        entity.setActive(subCategory.isActive());
    }

    private SubCategory toDomain(SubCategoryEntityJpa entity) {
        return new SubCategory(entity.getId(), entity.getVersion(), entity.getCategoryId(), entity.getName(),
                entity.isActive(), null, null);
    }
}
