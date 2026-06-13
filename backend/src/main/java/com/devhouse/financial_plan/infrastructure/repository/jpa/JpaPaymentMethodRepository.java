package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPaymentMethodRepository extends JpaRepository<PaymentMethodEntityJpa, Long> {

    List<PaymentMethodEntityJpa> findBySpaceId(Long spaceId);
}
