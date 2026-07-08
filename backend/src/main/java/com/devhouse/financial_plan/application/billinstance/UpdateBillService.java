package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
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

    public BillInstanceResponse execute(Long id, UpdateBillInstanceRequest request) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill not found");
        }
        bill.setVersion(request.version());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());
        bill.updateDetails(request.name(), category, subCategory, request.amount(), request.dueDate());
        bill.validate();
        Bill updated = billRepository.update(bill);
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

    private BillInstanceResponse toResponse(Bill bill) {
        return new BillInstanceResponse(bill.getId(), bill.getVersion(),
                bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null, bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null,
                bill.getSubCategory() != null ? bill.getSubCategory().getId() : null, bill.getReferenceMonth(),
                bill.getDueDate(), bill.getAmount(), bill.getStatus(), bill.getPaidDate(), bill.getPaymentTransactionId(),
                bill.getBankAccountId(), bill.getCreatedDate());
    }
}
