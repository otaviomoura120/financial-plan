package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPaymentMethodsService {

    private final PaymentMethodRepository paymentMethodRepository;

    public ListPaymentMethodsService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<PaymentMethodResponse> execute(Long spaceId) {
        return paymentMethodRepository.findBySpaceId(spaceId).stream()
                .map(pm -> new PaymentMethodResponse(pm.getId(), pm.getVersion(), pm.getName(), pm.isActive()))
                .toList();
    }
}
