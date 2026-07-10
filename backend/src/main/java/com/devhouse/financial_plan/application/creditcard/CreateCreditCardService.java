package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.application.creditcard.dto.CreateCreditCardRequest;
import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateCreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final SpaceRepository spaceRepository;
    private final BankAccountRepository bankAccountRepository;

    public CreateCreditCardService(CreditCardRepository creditCardRepository, SpaceRepository spaceRepository,
                                   BankAccountRepository bankAccountRepository) {
        this.creditCardRepository = creditCardRepository;
        this.spaceRepository = spaceRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public CreditCardResponse execute(CreateCreditCardRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        if (space == null) {
            throw new DomainException("Space not found");
        }
        BankAccount bankAccount = resolveBankAccount(request.bankAccountId(), space.getId());
        CreditCard creditCard = new CreditCard(null, 0, space, bankAccount, request.name(), request.limit(),
                request.closingDay(), request.dueDay(), true, Instant.now(), null);
        creditCard.validate();
        CreditCard saved = creditCardRepository.save(creditCard);
        return toResponse(saved);
    }

    private BankAccount resolveBankAccount(Long bankAccountId, Long spaceId) {
        if (bankAccountId == null) {
            return null;
        }
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId);
        if (bankAccount == null) {
            throw new DomainException("Bank account not found");
        }
        if (bankAccount.getSpace() == null || !bankAccount.getSpace().getId().equals(spaceId)) {
            throw new DomainException("Bank account does not belong to the credit card space");
        }
        return bankAccount;
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
