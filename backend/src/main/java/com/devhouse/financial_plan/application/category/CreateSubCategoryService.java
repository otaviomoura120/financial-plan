package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CreateSubCategoryRequest;
import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    public CreateSubCategoryService(SubCategoryRepository subCategoryRepository, CategoryRepository categoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
    }

    public SubCategoryResponse execute(CreateSubCategoryRequest request) {
        categoryRepository.findById(request.categoryId());
        SubCategory subCategory = new SubCategory(null, 0, request.categoryId(), request.name(),
                true, Instant.now(), null);
        subCategory.validate();
        SubCategory saved = subCategoryRepository.save(subCategory);
        return new SubCategoryResponse(saved.getId(), saved.getVersion(), saved.getCategoryId(), saved.getName(), saved.isActive());
    }
}
