package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;

@Service
public class CreateBillService {

    private final BillRepository billRepository;
    private final BillInstanceRepository billInstanceRepository;
    private final SpaceRepository spaceRepository;
    private final CategoryRepository categoryRepository;

    public CreateBillService(BillRepository billRepository, BillInstanceRepository billInstanceRepository,
                              SpaceRepository spaceRepository, CategoryRepository categoryRepository) {
        this.billRepository = billRepository;
        this.billInstanceRepository = billInstanceRepository;
        this.spaceRepository = spaceRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public BillResponse execute(CreateBillRequest request) {
        Space space = resolveSpace(request.spaceId());
        Category category = resolveCategory(request.categoryId());

        Bill bill = new Bill(null, 0, space, request.name(), category, request.defaultAmount(),
                request.startDate(), request.recurring(), true, Instant.now(), null);
        bill.validate();
        Bill saved = billRepository.save(bill);

        if (!saved.isRecurring()) {
            createSingleInstance(saved);
        }
        return toResponse(saved);
    }

    private void createSingleInstance(Bill bill) {
        YearMonth referenceMonth = YearMonth.from(bill.getStartDate());
        BillInstance instance = new BillInstance(null, 0, bill, referenceMonth.atDay(1), bill.getStartDate(),
                bill.getDefaultAmount(), BillInstanceStatus.PENDING, null, null, null, Instant.now(), null);
        instance.validate();
        billInstanceRepository.save(instance);
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

    private BillResponse toResponse(Bill bill) {
        return new BillResponse(bill.getId(), bill.getVersion(), bill.getSpace().getId(), bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null, bill.getDefaultAmount(),
                bill.getStartDate(), bill.isRecurring(), bill.isActive(), bill.getCreatedDate());
    }
}
