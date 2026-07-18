package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
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

    public CreateBillService(BillRecurringRepository billRecurringRepository, SpaceRepository spaceRepository,
                              CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.spaceRepository = spaceRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    public BillResponse execute(CreateBillRequest request) {
        Space space = resolveSpace(request.spaceId());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());

        BillRecurring billRecurring = new BillRecurring(null, 0, space, request.name(), category, subCategory,
                request.defaultAmount(), request.startDate(), true, Instant.now(), null);
        billRecurring.validate();
        BillRecurring saved = billRecurringRepository.save(billRecurring);
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

    private BillResponse toResponse(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.isActive(),
                billRecurring.getCreatedDate());
    }
}
