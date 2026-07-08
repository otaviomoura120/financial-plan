package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillService {

    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;

    public UpdateBillService(BillRepository billRepository, CategoryRepository categoryRepository) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
    }

    public BillResponse execute(Long id, UpdateBillRequest request) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill not found");
        }
        bill.setVersion(request.version());
        Category category = resolveCategory(request.categoryId());
        bill.update(request.name(), category, request.defaultAmount());
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

    private BillResponse toResponse(Bill bill) {
        return new BillResponse(bill.getId(), bill.getVersion(), bill.getSpace().getId(), bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null, bill.getDefaultAmount(),
                bill.getStartDate(), bill.isRecurring(), bill.isActive(), bill.getCreatedDate());
    }
}
