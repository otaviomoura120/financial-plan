package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.application.paymentmethod.dto.UpdatePaymentMethodRequest;
import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdatePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public UpdatePaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethodResponse execute(Long id, UpdatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id);
        paymentMethod.rename(request.name());
        paymentMethod.validate();
        PaymentMethod updated = paymentMethodRepository.update(paymentMethod);
        return new PaymentMethodResponse(updated.getId(), updated.getName(), updated.isActive());
    }
}
