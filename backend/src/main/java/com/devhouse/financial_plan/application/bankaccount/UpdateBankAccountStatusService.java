package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBankAccountStatusService {

    private final BankAccountRepository bankAccountRepository;

    public UpdateBankAccountStatusService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public BankAccountResponse execute(Long id, boolean active) {
        BankAccount account = bankAccountRepository.findById(id);
        if (active) {
            account.activate();
        } else {
            account.deactivate();
        }
        BankAccount updated = bankAccountRepository.update(account);
        return new BankAccountResponse(updated.getId(), updated.getVersion(), updated.getSpace().getId(), updated.getName(),
                updated.getBankName(), updated.getBalance(), updated.isActive(), updated.getCreatedDate());
    }
}
