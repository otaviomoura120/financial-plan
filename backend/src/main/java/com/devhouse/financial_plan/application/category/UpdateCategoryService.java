package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CategoryResponse;
import com.devhouse.financial_plan.application.category.dto.UpdateCategoryRequest;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UpdateCategoryService {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse execute(Long id, UpdateCategoryRequest request) {
        var category = categoryRepository.findById(id);
        category.setName(request.name());
        category.setUpdatedDate(Instant.now());
        category.validate();
        var updated = categoryRepository.update(category);
        return new CategoryResponse(updated.getId(), updated.getName(), updated.isActive(), List.of());
    }
}
