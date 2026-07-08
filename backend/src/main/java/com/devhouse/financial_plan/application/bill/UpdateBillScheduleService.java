package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillScheduleRequest;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillScheduleService {

    private final BillRepository billRepository;

    public UpdateBillScheduleService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public BillResponse execute(Long id, UpdateBillScheduleRequest request) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill not found");
        }
        bill.setVersion(request.version());
        bill.updateSchedule(request.recurring(), request.startDate());
        bill.validate();
        Bill updated = billRepository.update(bill);
        return toResponse(updated);
    }

    private BillResponse toResponse(Bill bill) {
        return new BillResponse(bill.getId(), bill.getVersion(), bill.getSpace().getId(), bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null, bill.getDefaultAmount(),
                bill.getStartDate(), bill.isRecurring(), bill.isActive(), bill.getCreatedDate());
    }
}
