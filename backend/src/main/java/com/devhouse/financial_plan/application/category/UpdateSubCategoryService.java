package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.application.category.dto.UpdateSubCategoryRequest;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryNameValidator categoryNameValidator;

    public UpdateSubCategoryService(SubCategoryRepository subCategoryRepository, CategoryNameValidator categoryNameValidator) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryNameValidator = categoryNameValidator;
    }

    public SubCategoryResponse execute(Long id, UpdateSubCategoryRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(id);
        if (subCategory == null) {
            throw new DomainException("Sub category not found");
        }
        if (subCategory.isSystem()) {
            throw new DomainException("System subcategories cannot be modified");
        }
        subCategory.setVersion(request.version());
        categoryNameValidator.rejectDuplicatedSubCategoryName(subCategory.getCategory().getId(), request.name(), id);
        subCategory.rename(request.name());
        subCategory.validate();
        SubCategory updated = subCategoryRepository.update(subCategory);
        return new SubCategoryResponse(updated.getId(), updated.getVersion(), updated.getCategory().getId(),
                updated.getName(), updated.isActive(), updated.isSystem());
    }
}
