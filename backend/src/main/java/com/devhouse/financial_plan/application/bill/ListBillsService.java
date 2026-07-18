package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListBillsService {

    private final BillRecurringRepository billRecurringRepository;

    public ListBillsService(BillRecurringRepository billRecurringRepository) {
        this.billRecurringRepository = billRecurringRepository;
    }

    public List<BillResponse> execute(Long spaceId) {
        return billRecurringRepository.findActiveBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private BillResponse toResponse(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.isActive(),
                billRecurring.getCreatedDate());
    }
}
