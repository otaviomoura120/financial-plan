package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.domain.CreditCard;
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

    public void execute(Long spaceId, Long id) {
        resolveCreditCard(id, spaceId);
        if (creditCardTransactionRepository.existsByCreditCardId(id)) {
            throw new DomainException("Cannot delete credit card: there are transactions linked to it.");
        }
        if (creditCardInvoicePaymentRepository.existsByCreditCardId(id)) {
            throw new DomainException("Cannot delete credit card: there are paid invoices linked to it.");
        }
        creditCardRepository.delete(id);
    }

    private CreditCard resolveCreditCard(Long id, Long spaceId) {
        CreditCard creditCard = creditCardRepository.findById(id);
        if (creditCard == null || !creditCard.belongsToSpace(spaceId)) {
            throw new DomainException("Credit card not found");
        }
        return creditCard;
    }
}
