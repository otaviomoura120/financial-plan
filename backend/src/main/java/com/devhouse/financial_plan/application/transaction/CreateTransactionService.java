package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateTransactionService {

    private final TransactionRepository transactionRepository;

    public CreateTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse execute(CreateTransactionRequest request) {
        Transaction transaction = new Transaction(null, 0, request.type(), request.userId(),
                request.bankAccountId(), request.categoryId(), request.subCategoryId(),
                request.paymentMethodId(), request.amount(), request.transactionDate(),
                request.description(), Instant.now(), null);
        transaction.validate();
        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUserId(), t.getBankAccountId(),
                t.getCategoryId(), t.getSubCategoryId(), t.getPaymentMethodId(), t.getAmount(),
                t.getTransactionDate(), t.getDescription(), t.getCreatedDate());
    }
}
