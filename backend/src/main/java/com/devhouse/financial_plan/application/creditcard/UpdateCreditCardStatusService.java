package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.domain.exception.NotFoundException;
import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateCreditCardStatusService {

    private final CreditCardRepository creditCardRepository;

    public UpdateCreditCardStatusService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    public CreditCardResponse execute(Long spaceId, Long id, boolean active) {
        CreditCard creditCard = resolveCreditCard(id, spaceId);
        if (active) {
            creditCard.activate();
        } else {
            creditCard.deactivate();
        }
        CreditCard updated = creditCardRepository.update(creditCard);
        BankAccount bankAccount = updated.getBankAccount();
        return new CreditCardResponse(updated.getId(), updated.getVersion(), updated.getSpace().getId(),
                updated.getName(), updated.getLimit(), updated.getClosingDay(), updated.getDueDay(),
                updated.isActive(), updated.getCreatedDate(),
                bankAccount != null ? bankAccount.getId() : null,
                bankAccount != null ? bankAccount.getName() : null);
    }

    private CreditCard resolveCreditCard(Long id, Long spaceId) {
        CreditCard creditCard = creditCardRepository.findById(id);
        if (creditCard == null || !creditCard.belongsToSpace(spaceId)) {
            throw new NotFoundException("Credit card not found");
        }
        return creditCard;
    }
}
