package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BankAccountRepositoryImpl implements BankAccountRepository {

    @Override
    public BankAccount save(BankAccount bankAccount) { return null; }

    @Override
    public BankAccount update(BankAccount bankAccount) { return null; }

    @Override
    public BankAccount findById(Long id) { return null; }

    @Override
    public List<BankAccount> findByUserId(Long userId) { return List.of(); }

    @Override
    public void delete(Long id) {}
}
