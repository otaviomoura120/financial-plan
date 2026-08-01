package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.CategoryResponse;
import com.devhouse.financial_plan.application.category.dto.CreateCategoryRequest;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CreateCategoryService {

    private final CategoryRepository categoryRepository;
    private final SpaceRepository spaceRepository;
    private final CategoryNameValidator categoryNameValidator;

    public CreateCategoryService(CategoryRepository categoryRepository, SpaceRepository spaceRepository,
                                  CategoryNameValidator categoryNameValidator) {
        this.categoryRepository = categoryRepository;
        this.spaceRepository = spaceRepository;
        this.categoryNameValidator = categoryNameValidator;
    }

    public CategoryResponse execute(CreateCategoryRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        if (space == null) {
            throw new DomainException("Space not found");
        }
        Category category = new Category(null, 0, space, request.name(), true, Instant.now(), null);
        category.validate();
        categoryNameValidator.rejectReservedName(request.name());
        categoryNameValidator.rejectDuplicatedCategoryName(request.spaceId(), request.name(), null);
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getVersion(), saved.getName(), saved.isActive(),
                saved.isSystem(), List.of());
    }
}
