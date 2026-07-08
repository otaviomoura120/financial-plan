package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceAmountRequest;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBillInstanceAmountService {

    private final BillInstanceRepository billInstanceRepository;

    public UpdateBillInstanceAmountService(BillInstanceRepository billInstanceRepository) {
        this.billInstanceRepository = billInstanceRepository;
    }

    public BillInstanceResponse execute(Long id, UpdateBillInstanceAmountRequest request) {
        BillInstance instance = billInstanceRepository.findById(id);
        if (instance == null) {
            throw new DomainException("Bill instance not found");
        }
        instance.setVersion(request.version());
        instance.updateAmount(request.amount());
        BillInstance updated = billInstanceRepository.update(instance);
        return toResponse(updated);
    }

    private BillInstanceResponse toResponse(BillInstance instance) {
        return new BillInstanceResponse(instance.getId(), instance.getVersion(), instance.getBill().getId(),
                instance.getBill().getName(), instance.getReferenceMonth(), instance.getDueDate(), instance.getAmount(),
                instance.getStatus(), instance.getPaidDate(), instance.getPaymentTransactionId(), instance.getBankAccountId(),
                instance.getCreatedDate());
    }
}
