package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.application.creditcard.dto.UpdateCreditCardRequest;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateCreditCardService {

    private final CreditCardRepository creditCardRepository;

    public UpdateCreditCardService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    public CreditCardResponse execute(Long id, UpdateCreditCardRequest request) {
        CreditCard creditCard = creditCardRepository.findById(id);
        creditCard.setVersion(request.version());
        creditCard.update(request.name(), request.limit(), request.closingDay(), request.dueDay());
        creditCard.validate();
        CreditCard updated = creditCardRepository.update(creditCard);
        return toResponse(updated);
    }

    private CreditCardResponse toResponse(CreditCard creditCard) {
        return new CreditCardResponse(creditCard.getId(), creditCard.getVersion(), creditCard.getSpace().getId(),
                creditCard.getName(), creditCard.getLimit(), creditCard.getClosingDay(), creditCard.getDueDay(),
                creditCard.isActive(), creditCard.getCreatedDate());
    }
}
