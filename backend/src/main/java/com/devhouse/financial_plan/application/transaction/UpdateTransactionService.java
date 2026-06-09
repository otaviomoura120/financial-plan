package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateTransactionService {

    private final TransactionRepository transactionRepository;

    public UpdateTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse execute(Long id, UpdateTransactionRequest request) {
        var transaction = transactionRepository.findById(id);
        transaction.setType(request.type());
        transaction.setBankAccountId(request.bankAccountId());
        transaction.setCategoryId(request.categoryId());
        transaction.setSubCategoryId(request.subCategoryId());
        transaction.setPaymentMethodId(request.paymentMethodId());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description());
        transaction.setUpdatedDate(Instant.now());
        transaction.validate();
        var updated = transactionRepository.update(transaction);
        return new TransactionResponse(updated.getId(), updated.getType(), updated.getUserId(), updated.getBankAccountId(), updated.getCategoryId(), updated.getSubCategoryId(), updated.getPaymentMethodId(), updated.getAmount(), updated.getTransactionDate(), updated.getDescription(), updated.getCreatedDate());
    }
}
