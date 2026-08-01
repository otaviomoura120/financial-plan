package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CategoryResponse;
import com.devhouse.financial_plan.application.category.dto.UpdateCategoryRequest;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryNameValidator categoryNameValidator;

    public UpdateCategoryService(CategoryRepository categoryRepository, CategoryNameValidator categoryNameValidator) {
        this.categoryRepository = categoryRepository;
        this.categoryNameValidator = categoryNameValidator;
    }

    public CategoryResponse execute(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id);
        if (category == null) {
            throw new DomainException("Category not found");
        }
        if (category.isSystem()) {
            throw new DomainException("System categories cannot be modified");
        }
        category.setVersion(request.version());
        categoryNameValidator.rejectReservedName(request.name());
        categoryNameValidator.rejectDuplicatedCategoryName(category.getSpace().getId(), request.name(), id);
        category.rename(request.name());
        category.validate();
        Category updated = categoryRepository.update(category);
        return new CategoryResponse(updated.getId(), updated.getVersion(), updated.getName(), updated.isActive(),
                updated.isSystem(), List.of());
    }
}
