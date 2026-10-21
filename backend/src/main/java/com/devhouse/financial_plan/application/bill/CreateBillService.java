package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.application.billinstance.EnsureRecurringBillsGeneratedService;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.CategoryKindPolicy;
import com.devhouse.financial_plan.domain.SystemCategoryPolicy;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreateBillService {

    private final BillRecurringRepository billRecurringRepository;
    private final SpaceRepository spaceRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;

    public CreateBillService(BillRecurringRepository billRecurringRepository, SpaceRepository spaceRepository,
                              CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                              EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService) {
        this.billRecurringRepository = billRecurringRepository;
        this.spaceRepository = spaceRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
    }

    @Transactional
    public BillResponse execute(Long spaceId, CreateBillRequest request) {
        Space space = resolveSpace(spaceId);
        Category category = resolveCategory(request.categoryId(), spaceId);
        SubCategory subCategory = resolveSubCategory(request.subCategoryId(), spaceId);
        SystemCategoryPolicy.rejectSystemSelection(category, subCategory);
        CategoryKindPolicy.requireExpense(category);

        BillRecurring billRecurring = new BillRecurring(null, 0, space, request.name(), category, subCategory,
                request.defaultAmount(), request.startDate(), request.endDate(), request.installments(), true,
                Instant.now(), null);
        billRecurring.validate();
        BillRecurring saved = billRecurringRepository.save(billRecurring);
        ensureRecurringBillsGeneratedService.executeForRecurring(saved);
        return BillResponse.from(saved);
    }

    private Space resolveSpace(Long spaceId) {
        Space space = spaceId != null ? spaceRepository.findById(spaceId) : null;
        if (space == null) {
            throw new DomainException("Space not found");
        }
        return space;
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
}
