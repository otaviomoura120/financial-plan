package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.billinstance.UndoBillInstancePaymentService;
import com.devhouse.financial_plan.application.creditcardinvoice.UndoCreditCardInvoicePaymentService;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeleteTransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionBalanceEffectService balanceEffectService;
    private final UndoBillInstancePaymentService undoBillInstancePaymentService;
    private final UndoCreditCardInvoicePaymentService undoCreditCardInvoicePaymentService;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public DeleteTransactionService(TransactionRepository transactionRepository, TransactionBalanceEffectService balanceEffectService,
                                     UndoBillInstancePaymentService undoBillInstancePaymentService,
                                     UndoCreditCardInvoicePaymentService undoCreditCardInvoicePaymentService,
                                     CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.transactionRepository = transactionRepository;
        this.balanceEffectService = balanceEffectService;
        this.undoBillInstancePaymentService = undoBillInstancePaymentService;
        this.undoCreditCardInvoicePaymentService = undoCreditCardInvoicePaymentService;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public void execute(Long id) {
        Transaction transaction = transactionRepository.findById(id);
        if (transaction == null) {
            throw new DomainException("Transaction not found");
        }
        if (transaction.isLinkedToSource()) {
            undoLinkedPayment(transaction);
            return;
        }
        balanceEffectService.revert(transaction);
        transactionRepository.delete(id);
    }

    private void undoLinkedPayment(Transaction transaction) {
        if (transaction.getSourceType() == TransactionSourceType.BILL_INSTANCE_PAYMENT) {
            undoBillInstancePaymentService.execute(transaction.getSourceId());
            return;
        }
        List<CreditCardInvoicePayment> payments = creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn(List.of(transaction.getId()));
        if (payments.isEmpty()) {
            throw new DomainException("Credit card invoice payment not found for this transaction");
        }
        undoCreditCardInvoicePaymentService.execute(transaction.getSourceId(), payments.get(0).getReferenceMonth());
    }
}
