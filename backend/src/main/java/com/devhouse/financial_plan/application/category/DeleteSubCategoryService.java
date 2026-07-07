package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteSubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final TransactionRepository transactionRepository;

    public DeleteSubCategoryService(SubCategoryRepository subCategoryRepository, TransactionRepository transactionRepository) {
        this.subCategoryRepository = subCategoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Long id) {
        if (transactionRepository.existsBySubCategoryId(id)) {
            throw new DomainException("Cannot delete subcategory: there are transactions linked to it.");
        }
        subCategoryRepository.delete(id);
    }
}
