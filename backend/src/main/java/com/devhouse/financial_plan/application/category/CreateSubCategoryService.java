package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CreateSubCategoryRequest;
import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryNameValidator categoryNameValidator;

    public CreateSubCategoryService(SubCategoryRepository subCategoryRepository, CategoryRepository categoryRepository,
                                     CategoryNameValidator categoryNameValidator) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.categoryNameValidator = categoryNameValidator;
    }

    public SubCategoryResponse execute(CreateSubCategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId());
        if (category == null) {
            throw new DomainException("Category not found");
        }
        if (category.isSystem()) {
            throw new DomainException("System categories cannot receive new subcategories");
        }
        SubCategory subCategory = new SubCategory(null, 0, category, request.name(),
                true, Instant.now(), null);
        subCategory.validate();
        categoryNameValidator.rejectDuplicatedSubCategoryName(request.categoryId(), request.name(), null);
        SubCategory saved = subCategoryRepository.save(subCategory);
        return new SubCategoryResponse(saved.getId(), saved.getVersion(), saved.getCategory().getId(), saved.getName(),
                saved.isActive(), saved.isSystem());
    }
}
