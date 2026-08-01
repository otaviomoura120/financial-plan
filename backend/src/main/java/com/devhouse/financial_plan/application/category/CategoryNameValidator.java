package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.enums.SystemCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryNameValidator {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public CategoryNameValidator(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public void rejectReservedName(String name) {
        if (SystemCategory.fromCategoryName(name) != null) {
            throw new DomainException("This category name is reserved by the system");
        }
    }

    public void rejectDuplicatedCategoryName(Long spaceId, String name, Long ignoredCategoryId) {
        List<Category> categories = categoryRepository.findBySpaceId(spaceId);
        boolean duplicated = categories.stream()
                .filter(existing -> !existing.getId().equals(ignoredCategoryId))
                .anyMatch(existing -> existing.hasSameName(name));
        if (duplicated) {
            throw new DomainException("There is already a category with this name");
        }
    }

    public void rejectDuplicatedSubCategoryName(Long categoryId, String name, Long ignoredSubCategoryId) {
        List<SubCategory> subCategories = subCategoryRepository.findByCategoryId(categoryId);
        boolean duplicated = subCategories.stream()
                .filter(existing -> !existing.getId().equals(ignoredSubCategoryId))
                .anyMatch(existing -> existing.hasSameName(name));
        if (duplicated) {
            throw new DomainException("There is already a subcategory with this name in this category");
        }
    }
}
