package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.PaymentMethod;

import java.util.List;

public interface PaymentMethodRepository {
    PaymentMethod save(PaymentMethod paymentMethod);
    PaymentMethod update(PaymentMethod paymentMethod);
    PaymentMethod findById(Long id);
    List<PaymentMethod> findAllActive();
    void delete(Long id);
}
