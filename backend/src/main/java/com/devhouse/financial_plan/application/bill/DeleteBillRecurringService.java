package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class DeleteBillRecurringService {

    private final BillRecurringRepository billRecurringRepository;
    private final BillRepository billRepository;

    public DeleteBillRecurringService(BillRecurringRepository billRecurringRepository, BillRepository billRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.billRepository = billRepository;
    }

    @Transactional
    public void execute(Long id) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        removeFutureAndDetachPastBills(id);
        billRecurringRepository.delete(id);
    }

    private void removeFutureAndDetachPastBills(Long billRecurringId) {
        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (Bill bill : billRepository.findByBillRecurringId(billRecurringId)) {
            if (isFromCurrentMonthOrLater(bill, currentMonth) && bill.isPending()) {
                billRepository.delete(bill.getId());
            }
            else {
                bill.detachFromRecurring();
                billRepository.update(bill);
            }
        }
    }

    private boolean isFromCurrentMonthOrLater(Bill bill, LocalDate currentMonth) {
        return !bill.getReferenceMonth().isBefore(currentMonth);
    }
}
