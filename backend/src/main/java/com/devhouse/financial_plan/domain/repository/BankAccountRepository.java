package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.BankAccount;

import java.util.List;

public interface BankAccountRepository {
    BankAccount save(BankAccount bankAccount);
    BankAccount update(BankAccount bankAccount);
    BankAccount findById(Long id);
    List<BankAccount> findByUserId(Long userId);
    void delete(Long id);
}
