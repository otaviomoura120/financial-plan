package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMethodRepositoryImpl implements PaymentMethodRepository {

    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) { return null; }

    @Override
    public PaymentMethod update(PaymentMethod paymentMethod) { return null; }

    @Override
    public PaymentMethod findById(Long id) { return null; }

    @Override
    public List<PaymentMethod> findAllActive() { return List.of(); }

    @Override
    public void delete(Long id) {}
}
