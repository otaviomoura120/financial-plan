package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.application.category.dto.UpdateSubCategoryRequest;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public UpdateSubCategoryService(SubCategoryRepository subCategoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
    }

    public SubCategoryResponse execute(Long id, UpdateSubCategoryRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(id);
        subCategory.setVersion(request.version());
        subCategory.rename(request.name());
        subCategory.validate();
        SubCategory updated = subCategoryRepository.update(subCategory);
        return new SubCategoryResponse(updated.getId(), updated.getVersion(), updated.getCategory().getId(), updated.getName(), updated.isActive());
    }
}
