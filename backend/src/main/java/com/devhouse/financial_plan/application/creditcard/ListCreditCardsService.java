package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCreditCardsService {

    private final CreditCardRepository creditCardRepository;

    public ListCreditCardsService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    public List<CreditCardResponse> execute(Long spaceId) {
        return creditCardRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CreditCardResponse toResponse(CreditCard creditCard) {
        BankAccount bankAccount = creditCard.getBankAccount();
        return new CreditCardResponse(creditCard.getId(), creditCard.getVersion(), creditCard.getSpace().getId(),
                creditCard.getName(), creditCard.getLimit(), creditCard.getClosingDay(), creditCard.getDueDay(),
                creditCard.isActive(), creditCard.getCreatedDate(),
                bankAccount != null ? bankAccount.getId() : null,
                bankAccount != null ? bankAccount.getName() : null);
    }
}
