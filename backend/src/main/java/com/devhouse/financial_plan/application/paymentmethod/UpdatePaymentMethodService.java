package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.application.paymentmethod.dto.UpdatePaymentMethodRequest;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdatePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public UpdatePaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethodResponse execute(Long id, UpdatePaymentMethodRequest request) {
        var paymentMethod = paymentMethodRepository.findById(id);
        paymentMethod.setName(request.name());
        paymentMethod.setUpdatedDate(Instant.now());
        paymentMethod.validate();
        var updated = paymentMethodRepository.update(paymentMethod);
        return new PaymentMethodResponse(updated.getId(), updated.getName(), updated.isActive());
    }
}
