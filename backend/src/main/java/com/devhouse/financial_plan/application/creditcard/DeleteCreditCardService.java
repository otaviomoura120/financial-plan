package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteCreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public DeleteCreditCardService(CreditCardRepository creditCardRepository, CreditCardTransactionRepository creditCardTransactionRepository,
                                    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardRepository = creditCardRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    public void execute(Long id) {
        if (creditCardTransactionRepository.existsByCreditCardId(id)) {
            throw new DomainException("Cannot delete credit card: there are transactions linked to it.");
        }
        if (creditCardInvoicePaymentRepository.existsByCreditCardId(id)) {
            throw new DomainException("Cannot delete credit card: there are paid invoices linked to it.");
        }
        creditCardRepository.delete(id);
    }
}
