package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.exception.NotFoundException;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateSubCategoryStatusService {

    private final SubCategoryRepository subCategoryRepository;

    public UpdateSubCategoryStatusService(SubCategoryRepository subCategoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
    }

    public SubCategoryResponse execute(Long spaceId, Long id, boolean active) {
        SubCategory subCategory = subCategoryRepository.findById(id);
        if (subCategory == null || !subCategory.belongsToSpace(spaceId)) {
            throw new NotFoundException("Sub category not found");
        }
        if (subCategory.isSystem()) {
            throw new DomainException("System subcategories cannot be modified");
        }
        if (active) {
            subCategory.activate();
        } else {
            subCategory.deactivate();
        }
        SubCategory updated = subCategoryRepository.update(subCategory);
        return new SubCategoryResponse(updated.getId(), updated.getVersion(), updated.getCategory().getId(),
                updated.getName(), updated.isActive(), updated.isSystem());
    }
}
