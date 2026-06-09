package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.application.bankaccount.dto.CreateBankAccountRequest;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateBankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public CreateBankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public BankAccountResponse execute(CreateBankAccountRequest request) {
        var account = new BankAccount(null, 0, request.userId(), request.name(), request.bankName(), request.initialBalance(), true, Instant.now(), null);
        account.validate();
        BankAccount saved = bankAccountRepository.save(account);
        return new BankAccountResponse(saved.getId(), saved.getUserId(), saved.getName(), saved.getBankName(), saved.getBalance(), saved.isActive(), saved.getCreatedDate());
    }
}
