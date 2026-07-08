package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;

@Service
public class EnsureRecurringBillsGeneratedService {

    private final BillRecurringRepository billRecurringRepository;
    private final BillRepository billRepository;

    public EnsureRecurringBillsGeneratedService(BillRecurringRepository billRecurringRepository, BillRepository billRepository) {
        this.billRecurringRepository = billRecurringRepository;
        this.billRepository = billRepository;
    }

    @Transactional
    public void execute(Long spaceId, LocalDate upToDate) {
        YearMonth capMonth = resolveCapMonth(upToDate);
        billRecurringRepository.findBySpaceId(spaceId).stream()
                .filter(BillRecurring::isActive)
                .forEach(billRecurring -> generateMissingInstances(billRecurring, capMonth));
    }

    private YearMonth resolveCapMonth(LocalDate upToDate) {
        YearMonth requestedMonth = YearMonth.from(upToDate);
        YearMonth maxAllowedMonth = YearMonth.now().plusMonths(1);
        return requestedMonth.isAfter(maxAllowedMonth) ? maxAllowedMonth : requestedMonth;
    }

    private void generateMissingInstances(BillRecurring billRecurring, YearMonth capMonth) {
        YearMonth startMonth = YearMonth.from(billRecurring.getStartDate());
        YearMonth lastGeneratedMonth = billRepository.findByBillRecurringId(billRecurring.getId()).stream()
                .map(Bill::getReferenceMonth)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(startMonth.minusMonths(1));

        YearMonth cursor = lastGeneratedMonth.plusMonths(1);
        if (cursor.isBefore(startMonth)) {
            cursor = startMonth;
        }
        while (!cursor.isAfter(capMonth)) {
            createInstanceIfMissing(billRecurring, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    private void createInstanceIfMissing(BillRecurring billRecurring, YearMonth referenceMonth) {
        if (billRepository.findByBillRecurringIdAndReferenceMonth(billRecurring.getId(), referenceMonth.atDay(1)) != null) {
            return;
        }
        LocalDate dueDate = resolveDueDate(billRecurring, referenceMonth);
        Bill bill = new Bill(null, 0, billRecurring.getSpace(), billRecurring, billRecurring.getName(),
                billRecurring.getCategory(), billRecurring.getSubCategory(), referenceMonth.atDay(1), dueDate,
                billRecurring.getDefaultAmount(), BillInstanceStatus.PENDING, null, null, null, false, Instant.now(), null);
        bill.validate();
        billRepository.save(bill);
    }

    private LocalDate resolveDueDate(BillRecurring billRecurring, YearMonth referenceMonth) {
        int dayOfMonth = Math.min(billRecurring.getStartDate().getDayOfMonth(), referenceMonth.lengthOfMonth());
        return referenceMonth.atDay(dayOfMonth);
    }
}
