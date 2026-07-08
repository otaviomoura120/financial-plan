package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillRecurringEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class BillRecurringRepositoryImpl implements BillRecurringRepository {

    private final JpaBillRecurringRepository jpaBillRecurringRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    public BillRecurringRepositoryImpl(JpaBillRecurringRepository jpaBillRecurringRepository, JpaSpaceRepository jpaSpaceRepository,
                                        JpaCategoryRepository jpaCategoryRepository, JpaSubCategoryRepository jpaSubCategoryRepository) {
        this.jpaBillRecurringRepository = jpaBillRecurringRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
    }

    @Override
    public BillRecurring save(BillRecurring billRecurring) {
        BillRecurringEntityJpa entity = new BillRecurringEntityJpa();
        applyFields(billRecurring, entity);
        entity.setCreatedAt(billRecurring.getCreatedDate());
        BillRecurringEntityJpa saved = jpaBillRecurringRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public BillRecurring update(BillRecurring billRecurring) {
        BillRecurringEntityJpa entity = jpaBillRecurringRepository.findById(billRecurring.getId()).orElseThrow();
        applyFields(billRecurring, entity);
        BillRecurringEntityJpa updated = jpaBillRecurringRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public BillRecurring findById(Long id) {
        return jpaBillRecurringRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<BillRecurring> findBySpaceId(Long spaceId) {
        return jpaBillRecurringRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaBillRecurringRepository.deleteById(id);
    }

    private void applyFields(BillRecurring billRecurring, BillRecurringEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(billRecurring.getSpace().getId()));
        entity.setName(billRecurring.getName());
        entity.setCategory(billRecurring.getCategory() != null ? jpaCategoryRepository.getReferenceById(billRecurring.getCategory().getId()) : null);
        entity.setSubCategory(billRecurring.getSubCategory() != null ? jpaSubCategoryRepository.getReferenceById(billRecurring.getSubCategory().getId()) : null);
        entity.setDefaultAmount(billRecurring.getDefaultAmount());
        entity.setStartDate(billRecurring.getStartDate());
        entity.setActive(billRecurring.isActive());
    }

    private BillRecurring toDomain(BillRecurringEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new BillRecurring(entity.getId(), entity.getVersion(), space, entity.getName(), category, subCategory,
                entity.getDefaultAmount(), entity.getStartDate(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private SubCategory buildSubCategory(SubCategoryEntityJpa entity) {
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        return new SubCategory(entity.getId(), entity.getVersion(), category, entity.getName(), entity.isActive(), null, null);
    }

    private Category buildCategory(CategoryEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new Category(entity.getId(), entity.getVersion(), space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
