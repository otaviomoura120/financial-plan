package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.application.category.dto.UpdateSubCategoryRequest;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public UpdateSubCategoryService(SubCategoryRepository subCategoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
    }

    public SubCategoryResponse execute(Long id, UpdateSubCategoryRequest request) {
        var subCategory = subCategoryRepository.findById(id);
        subCategory.setName(request.name());
        subCategory.setUpdatedDate(Instant.now());
        subCategory.validate();
        var updated = subCategoryRepository.update(subCategory);
        return new SubCategoryResponse(updated.getId(), updated.getCategoryId(), updated.getName(), updated.isActive());
    }
}
