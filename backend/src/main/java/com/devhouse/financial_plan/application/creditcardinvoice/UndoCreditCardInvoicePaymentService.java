package com.devhouse.financial_plan.application.creditcardinvoice;

import com.devhouse.financial_plan.application.transaction.TransactionBalanceEffectService;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UndoCreditCardInvoicePaymentService {

    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBalanceEffectService transactionBalanceEffectService;

    public UndoCreditCardInvoicePaymentService(CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository,
                                                TransactionRepository transactionRepository,
                                                TransactionBalanceEffectService transactionBalanceEffectService) {
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
        this.transactionRepository = transactionRepository;
        this.transactionBalanceEffectService = transactionBalanceEffectService;
    }

    @Transactional
    public void execute(Long creditCardId, LocalDate referenceMonth) {
        CreditCardInvoicePayment payment = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth);
        if (payment == null) {
            throw new DomainException("Credit card invoice payment not found");
        }
        Transaction transaction = transactionRepository.findById(payment.getPaymentTransactionId());
        transactionBalanceEffectService.revert(transaction);
        transactionRepository.delete(transaction.getId());
        creditCardInvoicePaymentRepository.deleteById(payment.getId());
    }
}
