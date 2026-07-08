package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;

@Service
public class DeactivateBillService {

    private final BillRepository billRepository;

    public DeactivateBillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public void execute(Long id) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill not found");
        }
        bill.deactivate();
        billRepository.update(bill);
    }
}
