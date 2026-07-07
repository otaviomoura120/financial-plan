package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionBalanceEffectService {

    private final BankAccountRepository bankAccountRepository;

    public TransactionBalanceEffectService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void apply(Transaction transaction) {
        if (transaction.isIncome()) {
            creditAccount(transaction.getBankAccount().getId(), transaction.getAmount());
        } else if (transaction.isExpense()) {
            debitAccount(transaction.getBankAccount().getId(), transaction.getAmount());
        } else if (transaction.isTransfer()) {
            debitAccount(transaction.getBankAccount().getId(), transaction.getAmount());
            creditAccount(transaction.getDestinationBankAccount().getId(), transaction.getAmount());
        }
    }

    public void revert(Transaction transaction) {
        if (transaction.isIncome()) {
            debitAccount(transaction.getBankAccount().getId(), transaction.getAmount());
        } else if (transaction.isExpense()) {
            creditAccount(transaction.getBankAccount().getId(), transaction.getAmount());
        } else if (transaction.isTransfer()) {
            creditAccount(transaction.getBankAccount().getId(), transaction.getAmount());
            debitAccount(transaction.getDestinationBankAccount().getId(), transaction.getAmount());
        }
    }

    private void creditAccount(Long bankAccountId, BigDecimal amount) {
        BankAccount account = bankAccountRepository.findById(bankAccountId);
        account.credit(amount);
        bankAccountRepository.update(account);
    }

    private void debitAccount(Long bankAccountId, BigDecimal amount) {
        BankAccount account = bankAccountRepository.findById(bankAccountId);
        account.debit(amount);
        bankAccountRepository.update(account);
    }
}
