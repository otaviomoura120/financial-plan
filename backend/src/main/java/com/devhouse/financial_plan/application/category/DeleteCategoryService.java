package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteCategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final TransactionRepository transactionRepository;

    public DeleteCategoryService(CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                  TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Long id) {
        if (subCategoryRepository.existsByCategoryId(id)) {
            throw new DomainException("Cannot delete category: there are subcategories linked to it.");
        }
        if (transactionRepository.existsByCategoryId(id)) {
            throw new DomainException("Cannot delete category: there are transactions linked to it.");
        }
        categoryRepository.delete(id);
    }
}
