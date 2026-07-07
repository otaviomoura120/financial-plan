package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdatePaymentMethodStatusService {

    private final PaymentMethodRepository paymentMethodRepository;

    public UpdatePaymentMethodStatusService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethodResponse execute(Long id, boolean active) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id);
        if (active) {
            paymentMethod.activate();
        } else {
            paymentMethod.deactivate();
        }
        PaymentMethod updated = paymentMethodRepository.update(paymentMethod);
        return new PaymentMethodResponse(updated.getId(), updated.getVersion(), updated.getName(), updated.isActive());
    }
}
