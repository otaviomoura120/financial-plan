package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillRecurringService {

    private final BillRecurringRepository billRecurringRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public UpdateBillRecurringService(BillRecurringRepository billRecurringRepository, CategoryRepository categoryRepository,
                                       SubCategoryRepository subCategoryRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public BillResponse execute(Long id, UpdateBillRecurringRequest request) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        billRecurring.setVersion(request.version());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());
        billRecurring.update(request.name(), category, subCategory, request.defaultAmount());
        billRecurring.validate();
        BillRecurring updated = billRecurringRepository.update(billRecurring);
        return toResponse(updated);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId);
        if (category == null) {
            throw new DomainException("Category not found");
        }
        return category;
    }

    private SubCategory resolveSubCategory(Long subCategoryId) {
        if (subCategoryId == null) {
            return null;
        }
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId);
        if (subCategory == null) {
            throw new DomainException("Sub category not found");
        }
        return subCategory;
    }

    private BillResponse toResponse(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.isActive(),
                billRecurring.getCreatedDate());
    }
}
