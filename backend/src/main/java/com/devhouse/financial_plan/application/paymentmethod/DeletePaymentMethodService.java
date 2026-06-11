package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

@Service
public class DeletePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public DeletePaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public void execute(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id);
        paymentMethod.deactivate();
        paymentMethodRepository.update(paymentMethod);
    }
}
