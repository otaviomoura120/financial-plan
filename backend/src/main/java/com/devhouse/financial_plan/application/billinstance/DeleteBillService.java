package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteBillService {

    private final BillRepository billRepository;

    public DeleteBillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public void execute(Long id) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill not found");
        }
        bill.markDeleted();
        billRepository.update(bill);
    }
}
