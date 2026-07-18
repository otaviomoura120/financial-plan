package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCreditCardRepository extends JpaRepository<CreditCardEntityJpa, Long> {

    List<CreditCardEntityJpa> findBySpaceId(Long spaceId);
}
