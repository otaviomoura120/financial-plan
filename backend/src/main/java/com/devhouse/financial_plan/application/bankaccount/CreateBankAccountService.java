package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.application.bankaccount.dto.CreateBankAccountRequest;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateBankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final SpaceRepository spaceRepository;

    public CreateBankAccountService(BankAccountRepository bankAccountRepository, SpaceRepository spaceRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.spaceRepository = spaceRepository;
    }

    public BankAccountResponse execute(CreateBankAccountRequest request) {
        Space space = spaceRepository.findById(request.spaceId());
        if (space == null) {
            throw new DomainException("Space not found");
        }
        BankAccount account = new BankAccount(null, 0, space, request.name(),
                request.bankName(), request.initialBalance(), true, Instant.now(), null);
        account.validate();
        BankAccount saved = bankAccountRepository.save(account);
        return new BankAccountResponse(saved.getId(), saved.getVersion(), saved.getSpace().getId(), saved.getName(),
                saved.getBankName(), saved.getBalance(), saved.isActive(), saved.getCreatedDate());
    }
}
