package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;

    public DeleteSubCategoryService(SubCategoryRepository subCategoryRepository) {
        this.subCategoryRepository = subCategoryRepository;
    }

    public void execute(Long id) {
        SubCategory subCategory = subCategoryRepository.findById(id);
        subCategory.deactivate();
        subCategoryRepository.update(subCategory);
    }
}
