package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateTransactionService {

    private final TransactionRepository transactionRepository;

    public UpdateTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse execute(Long id, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id);
        transaction.setVersion(request.version());
        transaction.update(request.type(), request.bankAccountId(), null, request.categoryId(),
                request.subCategoryId(), request.paymentMethodId(), request.amount(),
                request.transactionDate(), request.description());
        transaction.validate();
        Transaction updated = transactionRepository.update(transaction);
        return new TransactionResponse(updated.getId(), updated.getVersion(), updated.getType(), updated.getUserId(),
                updated.getBankAccountId(), updated.getCategoryId(), updated.getSubCategoryId(),
                updated.getPaymentMethodId(), updated.getAmount(), updated.getTransactionDate(),
                updated.getDescription(), updated.getCreatedDate());
    }
}
