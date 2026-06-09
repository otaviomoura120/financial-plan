package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteTransactionService {

    private final TransactionRepository transactionRepository;

    public DeleteTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void execute(Long id) {
        transactionRepository.delete(id);
    }
}
