package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface JpaCreditCardTransactionRepository extends JpaRepository<CreditCardTransactionEntityJpa, Long>,
        JpaSpecificationExecutor<CreditCardTransactionEntityJpa> {

    List<CreditCardTransactionEntityJpa> findByInstallmentGroupId(String installmentGroupId);
    List<CreditCardTransactionEntityJpa> findByCreditCard_Id(Long creditCardId);
    List<CreditCardTransactionEntityJpa> findByCreditCard_IdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth);
}
