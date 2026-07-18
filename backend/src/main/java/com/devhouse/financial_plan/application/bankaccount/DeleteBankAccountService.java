package com.devhouse.financial_plan.application.bankaccount;

import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteBankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public DeleteBankAccountService(BankAccountRepository bankAccountRepository, TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Long id) {
        if (transactionRepository.existsByBankAccountId(id)) {
            throw new DomainException("Cannot delete bank account: there are transactions linked to it.");
        }
        bankAccountRepository.delete(id);
    }
}
