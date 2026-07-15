package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class UpdateBillRecurringService {

    private final BillRecurringRepository billRecurringRepository;
    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public UpdateBillRecurringService(BillRecurringRepository billRecurringRepository, BillRepository billRepository,
                                       CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    public BillResponse execute(Long id, UpdateBillRecurringRequest request) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        billRecurring.setVersion(request.version());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());
        billRecurring.update(request.name(), category, subCategory, request.defaultAmount());
        billRecurring.updateSchedule(request.startDate());
        billRecurring.validate();
        BillRecurring updated = billRecurringRepository.update(billRecurring);
        updateCurrentAndFutureBills(updated);
        return toResponse(updated);
    }

    private void updateCurrentAndFutureBills(BillRecurring billRecurring) {
        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (Bill bill : billRepository.findByBillRecurringId(billRecurring.getId())) {
            if (isFromCurrentMonthOrLater(bill, currentMonth) && bill.isPending()) {
                LocalDate newDueDate = resolveDueDate(billRecurring, bill.getReferenceMonth());
                bill.updateDetails(billRecurring.getName(), billRecurring.getCategory(), billRecurring.getSubCategory(),
                        billRecurring.getDefaultAmount(), newDueDate);
                billRepository.update(bill);
            }
        }
    }

    private boolean isFromCurrentMonthOrLater(Bill bill, LocalDate currentMonth) {
        return !bill.getReferenceMonth().isBefore(currentMonth);
    }

    private LocalDate resolveDueDate(BillRecurring billRecurring, LocalDate referenceMonth) {
        YearMonth month = YearMonth.from(referenceMonth);
        int dayOfMonth = Math.min(billRecurring.getStartDate().getDayOfMonth(), month.lengthOfMonth());
        return month.atDay(dayOfMonth);
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
