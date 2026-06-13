package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.application.paymentmethod.dto.CreatePaymentMethodRequest;
import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse;
import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreatePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final SpaceRepository spaceRepository;

    public CreatePaymentMethodService(PaymentMethodRepository paymentMethodRepository, SpaceRepository spaceRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.spaceRepository = spaceRepository;
    }

    public PaymentMethodResponse execute(CreatePaymentMethodRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        if (space == null) {
            throw new DomainException("Space not found");
        }
        PaymentMethod paymentMethod = new PaymentMethod(null, 0, space, request.name(), true, Instant.now(), null);
        paymentMethod.validate();
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return new PaymentMethodResponse(saved.getId(), saved.getName(), saved.isActive());
    }
}
