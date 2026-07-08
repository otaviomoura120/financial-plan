package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;

@Service
public class EnsureBillInstancesGeneratedService {

    private final BillRepository billRepository;
    private final BillInstanceRepository billInstanceRepository;

    public EnsureBillInstancesGeneratedService(BillRepository billRepository, BillInstanceRepository billInstanceRepository) {
        this.billRepository = billRepository;
        this.billInstanceRepository = billInstanceRepository;
    }

    @Transactional
    public void execute(Long spaceId, LocalDate upToDate) {
        YearMonth capMonth = resolveCapMonth(upToDate);
        billRepository.findBySpaceId(spaceId).stream()
                .filter(bill -> bill.isActive() && bill.isRecurring())
                .forEach(bill -> generateMissingInstances(bill, capMonth));
    }

    private YearMonth resolveCapMonth(LocalDate upToDate) {
        YearMonth requestedMonth = YearMonth.from(upToDate);
        YearMonth maxAllowedMonth = YearMonth.now().plusMonths(1);
        return requestedMonth.isAfter(maxAllowedMonth) ? maxAllowedMonth : requestedMonth;
    }

    private void generateMissingInstances(Bill bill, YearMonth capMonth) {
        YearMonth startMonth = YearMonth.from(bill.getStartDate());
        YearMonth lastGeneratedMonth = billInstanceRepository.findByBillId(bill.getId()).stream()
                .map(BillInstance::getReferenceMonth)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(startMonth.minusMonths(1));

        YearMonth cursor = lastGeneratedMonth.plusMonths(1);
        if (cursor.isBefore(startMonth)) {
            cursor = startMonth;
        }
        while (!cursor.isAfter(capMonth)) {
            createInstanceIfMissing(bill, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    private void createInstanceIfMissing(Bill bill, YearMonth referenceMonth) {
        if (billInstanceRepository.findByBillIdAndReferenceMonth(bill.getId(), referenceMonth.atDay(1)) != null) {
            return;
        }
        LocalDate dueDate = resolveDueDate(bill, referenceMonth);
        BillInstance instance = new BillInstance(null, 0, bill, referenceMonth.atDay(1), dueDate,
                bill.getDefaultAmount(), BillInstanceStatus.PENDING, null, null, null, Instant.now(), null);
        instance.validate();
        billInstanceRepository.save(instance);
    }

    private LocalDate resolveDueDate(Bill bill, YearMonth referenceMonth) {
        int dayOfMonth = Math.min(bill.getStartDate().getDayOfMonth(), referenceMonth.lengthOfMonth());
        return referenceMonth.atDay(dayOfMonth);
    }
}
