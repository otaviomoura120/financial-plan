package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCreditCardTransactionService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public DeleteCreditCardTransactionService(CreditCardTransactionRepository creditCardTransactionRepository,
                                               CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public void execute(Long id) {
        CreditCardTransaction transaction = creditCardTransactionRepository.findById(id);
        if (transaction == null) {
            throw new DomainException("Credit card transaction not found");
        }
        boolean alreadyPaid = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                transaction.getCreditCard().getId(), transaction.getReferenceMonth()) != null;
        if (alreadyPaid) {
            throw new DomainException("Cannot delete a transaction from a paid invoice");
        }
        creditCardTransactionRepository.delete(id);
    }
}
