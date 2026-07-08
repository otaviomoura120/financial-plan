package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListBillInstancesService {

    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;
    private final BillRepository billRepository;

    public ListBillInstancesService(EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService,
                                     BillRepository billRepository) {
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
        this.billRepository = billRepository;
    }

    public List<BillInstanceResponse> execute(Long spaceId, LocalDate from, LocalDate to) {
        LocalDate upToDate = to != null ? to : LocalDate.now();
        ensureRecurringBillsGeneratedService.execute(spaceId, upToDate);
        return billRepository.findBySpaceAndPeriod(spaceId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private BillInstanceResponse toResponse(Bill bill) {
        return new BillInstanceResponse(bill.getId(), bill.getVersion(),
                bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null, bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null,
                bill.getSubCategory() != null ? bill.getSubCategory().getId() : null, bill.getReferenceMonth(),
                bill.getDueDate(), bill.getAmount(), bill.getStatus(), bill.getPaidDate(), bill.getPaymentTransactionId(),
                bill.getBankAccountId(), bill.getCreatedDate());
    }
}
