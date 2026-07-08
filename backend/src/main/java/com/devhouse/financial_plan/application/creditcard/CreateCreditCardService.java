package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.application.creditcard.dto.CreateCreditCardRequest;
import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateCreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final SpaceRepository spaceRepository;

    public CreateCreditCardService(CreditCardRepository creditCardRepository, SpaceRepository spaceRepository) {
        this.creditCardRepository = creditCardRepository;
        this.spaceRepository = spaceRepository;
    }

    public CreditCardResponse execute(CreateCreditCardRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        if (space == null) {
            throw new DomainException("Space not found");
        }
        CreditCard creditCard = new CreditCard(null, 0, space, request.name(), request.limit(),
                request.closingDay(), request.dueDay(), true, Instant.now(), null);
        creditCard.validate();
        CreditCard saved = creditCardRepository.save(creditCard);
        return toResponse(saved);
    }

    private CreditCardResponse toResponse(CreditCard creditCard) {
        return new CreditCardResponse(creditCard.getId(), creditCard.getVersion(), creditCard.getSpace().getId(),
                creditCard.getName(), creditCard.getLimit(), creditCard.getClosingDay(), creditCard.getDueDay(),
                creditCard.isActive(), creditCard.getCreatedDate());
    }
}
