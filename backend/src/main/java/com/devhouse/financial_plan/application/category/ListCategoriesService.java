package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CategoryResponse;
import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCategoriesService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public ListCategoriesService(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public List<CategoryResponse> execute(Long spaceId) {
        return categoryRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        List<SubCategoryResponse> subCategories = subCategoryRepository.findByCategoryId(category.getId()).stream()
                .map(this::toSubCategoryResponse)
                .toList();
        return new CategoryResponse(category.getId(), category.getVersion(), category.getName(), category.isActive(), subCategories);
    }

    private SubCategoryResponse toSubCategoryResponse(SubCategory subCategory) {
        return new SubCategoryResponse(subCategory.getId(), subCategory.getVersion(), subCategory.getCategoryId(),
                subCategory.getName(), subCategory.isActive());
    }
}
