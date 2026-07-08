package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListBillInstancesService {

    private final EnsureBillInstancesGeneratedService ensureBillInstancesGeneratedService;
    private final BillInstanceRepository billInstanceRepository;

    public ListBillInstancesService(EnsureBillInstancesGeneratedService ensureBillInstancesGeneratedService,
                                     BillInstanceRepository billInstanceRepository) {
        this.ensureBillInstancesGeneratedService = ensureBillInstancesGeneratedService;
        this.billInstanceRepository = billInstanceRepository;
    }

    public List<BillInstanceResponse> execute(Long spaceId, LocalDate from, LocalDate to) {
        LocalDate upToDate = to != null ? to : LocalDate.now();
        ensureBillInstancesGeneratedService.execute(spaceId, upToDate);
        return billInstanceRepository.findBySpaceAndPeriod(spaceId, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private BillInstanceResponse toResponse(BillInstance instance) {
        return new BillInstanceResponse(instance.getId(), instance.getVersion(), instance.getBill().getId(),
                instance.getBill().getName(), instance.getReferenceMonth(), instance.getDueDate(), instance.getAmount(),
                instance.getStatus(), instance.getPaidDate(), instance.getPaymentTransactionId(), instance.getBankAccountId(),
                instance.getCreatedDate());
    }
}
