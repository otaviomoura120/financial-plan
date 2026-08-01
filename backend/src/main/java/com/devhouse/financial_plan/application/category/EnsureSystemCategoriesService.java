package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.enums.SystemCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Provisions the reserved categories used by system generated transactions (credit card invoice payments and
 * transfers). A category already named after a reserved name is adopted instead of duplicated, which keeps the
 * operation idempotent.
 */
@Service
public class EnsureSystemCategoriesService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SpaceRepository spaceRepository;

    public EnsureSystemCategoriesService(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                          SpaceRepository spaceRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.spaceRepository = spaceRepository;
    }

    @Transactional
    public void execute(Long spaceId) {
        List<Category> existingCategories = categoryRepository.findBySpaceId(spaceId);
        for (SystemCategory systemCategory : SystemCategory.values()) {
            Category category = findByName(existingCategories, systemCategory.getCategoryName());
            if (category == null) {
                category = createCategory(spaceId, systemCategory);
            }
            ensureSubCategory(category, systemCategory);
        }
    }

    private Category findByName(List<Category> categories, String name) {
        return categories.stream()
                .filter(category -> category.hasSameName(name))
                .findFirst()
                .orElse(null);
    }

    private Category createCategory(Long spaceId, SystemCategory systemCategory) {
        Space space = spaceRepository.findById(spaceId);
        if (space == null) {
            throw new DomainException("Space not found");
        }
        Category category = new Category(null, 0, space, systemCategory.getCategoryName(), true, Instant.now(), null);
        category.validate();
        return categoryRepository.save(category);
    }

    private void ensureSubCategory(Category category, SystemCategory systemCategory) {
        if (!systemCategory.hasSubCategory()) {
            return;
        }
        boolean alreadyExists = subCategoryRepository.findByCategoryId(category.getId()).stream()
                .anyMatch(existing -> existing.hasSameName(systemCategory.getSubCategoryName()));
        if (alreadyExists) {
            return;
        }
        SubCategory subCategory = new SubCategory(null, 0, category, systemCategory.getSubCategoryName(), true,
                Instant.now(), null);
        subCategory.validate();
        subCategoryRepository.save(subCategory);
    }
}
