package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaCreditCardInvoicePaymentRepository extends JpaRepository<CreditCardInvoicePaymentEntityJpa, Long> {

    Optional<CreditCardInvoicePaymentEntityJpa> findByCreditCard_IdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth);
}
