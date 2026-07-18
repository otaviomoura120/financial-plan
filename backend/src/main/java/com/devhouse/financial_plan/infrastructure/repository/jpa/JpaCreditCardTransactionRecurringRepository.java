package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JpaCreditCardTransactionRecurringRepository extends JpaRepository<CreditCardTransactionRecurringEntityJpa, Long>,
        JpaSpecificationExecutor<CreditCardTransactionRecurringEntityJpa> {

    List<CreditCardTransactionRecurringEntityJpa> findByCreditCard_Id(Long creditCardId);
}
