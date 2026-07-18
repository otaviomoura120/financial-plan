package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CreditCardInvoicePaymentResolver {

    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public CreditCardInvoicePaymentResolver(CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    public Map<Long, LocalDate> resolveInvoiceReferenceMonths(List<Transaction> transactions) {
        List<Long> invoicePaymentTransactionIds = transactions.stream()
                .filter(t -> TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT.equals(t.getSourceType()))
                .map(Transaction::getId)
                .toList();
        if (invoicePaymentTransactionIds.isEmpty()) {
            return Map.of();
        }
        return creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn(invoicePaymentTransactionIds).stream()
                .collect(Collectors.toMap(CreditCardInvoicePayment::getPaymentTransactionId, CreditCardInvoicePayment::getReferenceMonth));
    }
}
