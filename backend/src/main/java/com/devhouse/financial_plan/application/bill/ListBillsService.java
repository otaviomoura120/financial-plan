package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListBillsService {

    private final BillRepository billRepository;

    public ListBillsService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public List<BillResponse> execute(Long spaceId) {
        return billRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private BillResponse toResponse(Bill bill) {
        return new BillResponse(bill.getId(), bill.getVersion(), bill.getSpace().getId(), bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null, bill.getDefaultAmount(),
                bill.getStartDate(), bill.isRecurring(), bill.isActive(), bill.getCreatedDate());
    }
}
