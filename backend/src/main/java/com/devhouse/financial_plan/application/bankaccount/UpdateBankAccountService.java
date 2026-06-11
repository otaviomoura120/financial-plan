package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.application.bankaccount.dto.UpdateBankAccountRequest;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateBankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public UpdateBankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public BankAccountResponse execute(Long id, UpdateBankAccountRequest request) {
        BankAccount account = bankAccountRepository.findById(id);
        account.update(request.name(), request.bankName());
        account.validate();
        BankAccount updated = bankAccountRepository.update(account);
        return new BankAccountResponse(updated.getId(), updated.getUserId(), updated.getName(),
                updated.getBankName(), updated.getBalance(), updated.isActive(), updated.getCreatedDate());
    }
}
