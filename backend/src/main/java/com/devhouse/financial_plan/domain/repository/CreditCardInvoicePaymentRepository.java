package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;

import java.time.LocalDate;
import java.util.List;

public interface CreditCardInvoicePaymentRepository {
    CreditCardInvoicePayment save(CreditCardInvoicePayment creditCardInvoicePayment);
    CreditCardInvoicePayment findById(Long id);
    CreditCardInvoicePayment findByCreditCardIdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth);
    List<CreditCardInvoicePayment> findByPaymentTransactionIdIn(List<Long> transactionIds);
    void deleteById(Long id);
}
