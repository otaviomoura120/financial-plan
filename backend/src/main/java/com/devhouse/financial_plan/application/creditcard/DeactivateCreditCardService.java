package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import org.springframework.stereotype.Service;

@Service
public class DeactivateCreditCardService {

    private final CreditCardRepository creditCardRepository;

    public DeactivateCreditCardService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    public void execute(Long id) {
        CreditCard creditCard = creditCardRepository.findById(id);
        if (creditCard == null) {
            throw new DomainException("Credit card not found");
        }
        creditCard.deactivate();
        creditCardRepository.update(creditCard);
    }
}
