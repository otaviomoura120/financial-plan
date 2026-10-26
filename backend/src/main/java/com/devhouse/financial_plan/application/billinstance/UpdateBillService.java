package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.CategoryKindPolicy;
import com.devhouse.financial_plan.domain.SystemCategoryPolicy;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.exception.NotFoundException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillService {

    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public UpdateBillService(BillRepository billRepository, CategoryRepository categoryRepository,
                              SubCategoryRepository subCategoryRepository) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public BillInstanceResponse execute(Long spaceId, Long id, UpdateBillInstanceRequest request) {
        Bill bill = billRepository.findById(id);
        if (bill == null || !bill.belongsToSpace(spaceId)) {
            throw new NotFoundException("Bill not found");
        }
        bill.setVersion(request.version());
        Category category = resolveCategory(request.categoryId(), spaceId);
        SubCategory subCategory = resolveSubCategory(request.subCategoryId(), spaceId);
        SystemCategoryPolicy.rejectSystemSelection(category, subCategory);
        CategoryKindPolicy.requireExpense(category);
        bill.updateDetails(request.name(), category, subCategory, request.amount(), request.dueDate());
        bill.validate();
        Bill updated = billRepository.update(bill);
        return toResponse(updated);
    }

    private Category resolveCategory(Long categoryId, Long spaceId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId);
        if (category == null || !category.belongsToSpace(spaceId)) {
            throw new DomainException("Category not found");
        }
        return category;
    }

    private SubCategory resolveSubCategory(Long subCategoryId, Long spaceId) {
        if (subCategoryId == null) {
            return null;
        }
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId);
        if (subCategory == null || !subCategory.belongsToSpace(spaceId)) {
            throw new DomainException("Sub category not found");
        }
        return subCategory;
    }

    private BillInstanceResponse toResponse(Bill bill) {
        return new BillInstanceResponse(bill.getId(), bill.getVersion(),
                bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null, bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null,
                bill.getSubCategory() != null ? bill.getSubCategory().getId() : null, bill.getReferenceMonth(),
                bill.getDueDate(), bill.getAmount(), bill.getStatus(), bill.getPaidDate(), bill.getPaymentTransactionId(),
                bill.getBankAccountId(), bill.getCreatedDate());
    }
}
