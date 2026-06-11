package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.CreatePaymentMethodRequest;
import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreatePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public CreatePaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PaymentMethodResponse execute(CreatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = new PaymentMethod(null, 0, request.name(), true, Instant.now(), null);
        paymentMethod.validate();
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return new PaymentMethodResponse(saved.getId(), saved.getName(), saved.isActive());
    }
}
