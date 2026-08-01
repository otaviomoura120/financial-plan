package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.SystemCategoryPair;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.enums.SystemCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveSystemCategoryService {

    private final EnsureSystemCategoriesService ensureSystemCategoriesService;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public ResolveSystemCategoryService(EnsureSystemCategoriesService ensureSystemCategoriesService,
                                         CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.ensureSystemCategoriesService = ensureSystemCategoriesService;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    public SystemCategoryPair execute(Long spaceId, SystemCategory systemCategory) {
        ensureSystemCategoriesService.execute(spaceId);
        Category category = findCategory(spaceId, systemCategory);
        return new SystemCategoryPair(category.getId(), findSubCategoryId(category, systemCategory));
    }

    private Category findCategory(Long spaceId, SystemCategory systemCategory) {
        Category category = categoryRepository.findBySpaceId(spaceId).stream()
                .filter(existing -> existing.hasSameName(systemCategory.getCategoryName()))
                .findFirst()
                .orElse(null);
        if (category == null) {
            throw new DomainException("System category not found");
        }
        return category;
    }

    private Long findSubCategoryId(Category category, SystemCategory systemCategory) {
        if (!systemCategory.hasSubCategory()) {
            return null;
        }
        return subCategoryRepository.findByCategoryId(category.getId()).stream()
                .filter(existing -> existing.hasSameName(systemCategory.getSubCategoryName()))
                .map(SubCategory::getId)
                .findFirst()
                .orElse(null);
    }
}
