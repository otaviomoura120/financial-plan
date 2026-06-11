package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteBankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public DeleteBankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void execute(Long id) {
        BankAccount account = bankAccountRepository.findById(id);
        account.deactivate();
        bankAccountRepository.update(account);
    }
}
