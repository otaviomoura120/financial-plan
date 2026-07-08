package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import org.springframework.stereotype.Service;

@Service
public class DeactivateBillRecurringService {

    private final BillRecurringRepository billRecurringRepository;

    public DeactivateBillRecurringService(BillRecurringRepository billRecurringRepository) {
        this.billRecurringRepository = billRecurringRepository;
    }

    public void execute(Long id) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        billRecurring.deactivate();
        billRecurringRepository.update(billRecurring);
    }
}
