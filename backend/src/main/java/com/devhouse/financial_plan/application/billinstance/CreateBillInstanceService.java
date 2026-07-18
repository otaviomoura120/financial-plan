package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.CreateBillInstanceRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;

@Service
public class CreateBillInstanceService {

    private final BillRepository billRepository;
    private final SpaceRepository spaceRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public CreateBillInstanceService(BillRepository billRepository, SpaceRepository spaceRepository,
                                      CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.billRepository = billRepository;
        this.spaceRepository = spaceRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    public BillInstanceResponse execute(CreateBillInstanceRequest request) {
        Space space = resolveSpace(request.spaceId());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());
        YearMonth referenceMonth = YearMonth.from(request.dueDate());

        Bill bill = new Bill(null, 0, space, null, request.name(), category, subCategory, referenceMonth.atDay(1),
                request.dueDate(), request.amount(), BillInstanceStatus.PENDING, null, null, null, false, Instant.now(), null);
        bill.validate();
        Bill saved = billRepository.save(bill);
        return toResponse(saved);
    }

    private Space resolveSpace(Long spaceId) {
        Space space = spaceId != null ? spaceRepository.findById(spaceId) : null;
        if (space == null) {
            throw new DomainException("Space not found");
        }
        return space;
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
