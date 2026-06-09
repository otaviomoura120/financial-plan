package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteCategoryService {

    private final CategoryRepository categoryRepository;

    public DeleteCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void execute(Long id) {
        var category = categoryRepository.findById(id);
        category.deactivate();
        categoryRepository.update(category);
    }
}
