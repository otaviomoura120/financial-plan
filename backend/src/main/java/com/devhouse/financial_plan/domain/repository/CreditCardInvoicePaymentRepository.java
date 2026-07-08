package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;

import java.time.LocalDate;

public interface CreditCardInvoicePaymentRepository {
    CreditCardInvoicePayment save(CreditCardInvoicePayment creditCardInvoicePayment);
    CreditCardInvoicePayment findById(Long id);
    CreditCardInvoicePayment findByCreditCardIdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth);
    void deleteById(Long id);
}
