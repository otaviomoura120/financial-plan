package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListBankAccountsService {

    private final BankAccountRepository bankAccountRepository;

    public ListBankAccountsService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccountResponse> execute(Long spaceId) {
        return bankAccountRepository.findBySpaceId(spaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(account.getId(), account.getVersion(), account.getSpace().getId(), account.getName(),
                account.getBankName(), account.getBalance(), account.isActive(), account.getCreatedDate());
    }
}
