package com.devhouse.financial_plan.application.bill;

import com.devhouse.financial_plan.application.bill.dto.BillResponse;
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringScheduleRequest;
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
public class UpdateBillRecurringScheduleService {

    private final BillRecurringRepository billRecurringRepository;
    private final BillRepository billRepository;

    public UpdateBillRecurringScheduleService(BillRecurringRepository billRecurringRepository, BillRepository billRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.billRepository = billRepository;
    }

    @Transactional
    public BillResponse execute(Long id, UpdateBillRecurringScheduleRequest request) {
        BillRecurring billRecurring = billRecurringRepository.findById(id);
        if (billRecurring == null) {
            throw new DomainException("Bill recurring not found");
        }
        billRecurring.setVersion(request.version());
        billRecurring.updateSchedule(request.startDate());
        billRecurring.validate();
        BillRecurring updated = billRecurringRepository.update(billRecurring);
        updateCurrentAndFutureBillDueDates(updated);
        return toResponse(updated);
    }

    private void updateCurrentAndFutureBillDueDates(BillRecurring billRecurring) {
        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (Bill bill : billRepository.findByBillRecurringId(billRecurring.getId())) {
            if (isFromCurrentMonthOrLater(bill, currentMonth) && bill.isPending()) {
                LocalDate newDueDate = resolveDueDate(billRecurring, bill.getReferenceMonth());
                bill.updateDetails(bill.getName(), bill.getCategory(), bill.getSubCategory(), bill.getAmount(), newDueDate);
                billRepository.update(bill);
            }
        }
    }

    private boolean isFromCurrentMonthOrLater(Bill bill, LocalDate currentMonth) {
        return !bill.getReferenceMonth().isBefore(currentMonth);
    }

    private LocalDate resolveDueDate(BillRecurring billRecurring, LocalDate referenceMonth) {
        YearMonth month = YearMonth.from(referenceMonth);
        int dayOfMonth = Math.min(billRecurring.getStartDate().getDayOfMonth(), month.lengthOfMonth());
        return month.atDay(dayOfMonth);
    }

    private BillResponse toResponse(BillRecurring billRecurring) {
        return new BillResponse(billRecurring.getId(), billRecurring.getVersion(), billRecurring.getSpace().getId(),
                billRecurring.getName(), billRecurring.getCategory() != null ? billRecurring.getCategory().getId() : null,
                billRecurring.getSubCategory() != null ? billRecurring.getSubCategory().getId() : null,
                billRecurring.getDefaultAmount(), billRecurring.getStartDate(), billRecurring.isActive(),
                billRecurring.getCreatedDate());
    }
}
