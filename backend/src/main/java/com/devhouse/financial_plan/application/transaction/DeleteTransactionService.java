package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionBalanceEffectService balanceEffectService;

    public DeleteTransactionService(TransactionRepository transactionRepository, TransactionBalanceEffectService balanceEffectService) {
        this.transactionRepository = transactionRepository;
        this.balanceEffectService = balanceEffectService;
    }

    @Transactional
    public void execute(Long id) {
        Transaction transaction = transactionRepository.findById(id);
        if (transaction == null) {
            throw new DomainException("Transaction not found");
        }
        balanceEffectService.revert(transaction);
        transactionRepository.delete(id);
    }
}
