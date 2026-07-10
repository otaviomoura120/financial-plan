package com.devhouse.financial_plan.application.creditcard;

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse;
import com.devhouse.financial_plan.application.creditcard.dto.UpdateCreditCardRequest;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateCreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final BankAccountRepository bankAccountRepository;

    public UpdateCreditCardService(CreditCardRepository creditCardRepository, BankAccountRepository bankAccountRepository) {
        this.creditCardRepository = creditCardRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public CreditCardResponse execute(Long id, UpdateCreditCardRequest request) {
        CreditCard creditCard = creditCardRepository.findById(id);
        creditCard.setVersion(request.version());
        BankAccount bankAccount = resolveBankAccount(request.bankAccountId(), creditCard.getSpace().getId());
        creditCard.update(request.name(), request.limit(), request.closingDay(), request.dueDay(), bankAccount);
        creditCard.validate();
        CreditCard updated = creditCardRepository.update(creditCard);
        return toResponse(updated);
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
