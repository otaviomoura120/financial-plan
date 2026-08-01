package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class EnsureRecurringBillsGeneratedService {

    private final BillRecurringRepository billRecurringRepository;
    private final BillRepository billRepository;
    private final int horizonMonths;
    private final int maxHorizonMonths;

    public EnsureRecurringBillsGeneratedService(BillRecurringRepository billRecurringRepository, BillRepository billRepository,
                                                @Value("${bills.recurring.horizon-months:12}") int horizonMonths,
                                                @Value("${bills.recurring.max-horizon-months:60}") int maxHorizonMonths) {
        this.billRecurringRepository = billRecurringRepository;
        this.billRepository = billRepository;
        this.horizonMonths = horizonMonths;
        this.maxHorizonMonths = maxHorizonMonths;
    }

    @Transactional
    public void execute(Long spaceId, LocalDate upToDate) {
        YearMonth capMonth = resolveCapMonth(upToDate);
        billRecurringRepository.findBySpaceId(spaceId).stream()
                .filter(BillRecurring::isActive)
                .forEach(billRecurring -> generateMissingInstances(billRecurring, capMonth));
    }

    @Transactional
    public void executeForRecurring(BillRecurring billRecurring) {
        if (!billRecurring.isActive()) {
            return;
        }
        generateMissingInstances(billRecurring, defaultHorizonMonth());
    }

    private YearMonth defaultHorizonMonth() {
        return YearMonth.now().plusMonths(horizonMonths);
    }

    /**
     * Always keeps at least {@code horizonMonths} of future bills materialized. A period filter that
     * reaches further ahead is honoured, up to a safety ceiling so an extreme filter cannot create
     * thousands of rows.
     */
    private YearMonth resolveCapMonth(LocalDate upToDate) {
        YearMonth requestedMonth = YearMonth.from(upToDate);
        YearMonth horizonMonth = defaultHorizonMonth();
        YearMonth ceilingMonth = YearMonth.now().plusMonths(maxHorizonMonths);
        YearMonth capMonth = requestedMonth.isAfter(horizonMonth) ? requestedMonth : horizonMonth;
        return capMonth.isAfter(ceilingMonth) ? ceilingMonth : capMonth;
    }

    private void generateMissingInstances(BillRecurring billRecurring, YearMonth capMonth) {
        YearMonth effectiveCapMonth = applyRecurrenceEnd(billRecurring, capMonth);
        YearMonth startMonth = YearMonth.from(billRecurring.getStartDate());
        YearMonth cursor = resolveFirstMonthToGenerate(billRecurring, startMonth);
        while (!cursor.isAfter(effectiveCapMonth)) {
            createInstanceIfMissing(billRecurring, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    private YearMonth applyRecurrenceEnd(BillRecurring billRecurring, YearMonth capMonth) {
        YearMonth lastMonth = billRecurring.lastReferenceMonth();
        if (lastMonth == null) {
            return capMonth;
        }
        return capMonth.isAfter(lastMonth) ? lastMonth : capMonth;
    }

    private YearMonth resolveFirstMonthToGenerate(BillRecurring billRecurring, YearMonth startMonth) {
        List<Bill> existingBills = billRepository.findByBillRecurringId(billRecurring.getId());
        YearMonth lastGeneratedMonth = existingBills.stream()
                .map(Bill::getReferenceMonth)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(startMonth.minusMonths(1));

        YearMonth cursor = lastGeneratedMonth.plusMonths(1);
        return cursor.isBefore(startMonth) ? startMonth : cursor;
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
