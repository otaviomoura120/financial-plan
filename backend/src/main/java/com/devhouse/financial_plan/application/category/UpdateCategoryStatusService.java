package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CategoryResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateCategoryStatusService {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryStatusService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse execute(Long id, boolean active) {
        Category category = categoryRepository.findById(id);
        if (category == null) {
            throw new DomainException("Category not found");
        }
        if (category.isSystem()) {
            throw new DomainException("System categories cannot be modified");
        }
        if (active) {
            category.activate();
        } else {
            category.deactivate();
        }
        Category updated = categoryRepository.update(category);
        return new CategoryResponse(updated.getId(), updated.getVersion(), updated.getName(), updated.isActive(),
                updated.isSystem(), List.of());
    }
}
