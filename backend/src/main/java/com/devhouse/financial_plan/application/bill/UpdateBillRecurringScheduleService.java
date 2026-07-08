package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringScheduleRequest;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillRecurringScheduleService {

    private final BillRecurringRepository billRecurringRepository;

    public UpdateBillRecurringScheduleService(BillRecurringRepository billRecurringRepository) {
        this.billRecurringRepository = billRecurringRepository;
    }

    public BillResponse execute(Long id, UpdateBillRecurringScheduleRequest request) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        billRecurring.setVersion(request.version());
        billRecurring.updateSchedule(request.startDate());
        billRecurring.validate();
        BillRecurring updated = billRecurringRepository.update(billRecurring);
        return toResponse(updated);
    }

    private BillResponse toResponse(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.isActive(),
                billRecurring.getCreatedDate());
    }
}
