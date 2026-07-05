package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListTransactionsService {

    private final TransactionRepository transactionRepository;

    public ListTransactionsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionResponse> execute(Long spaceId, Long userId, Long bankAccountId, Long categoryId,
                                              Long subCategoryId, Long paymentMethodId, TransactionType type,
                                              LocalDate from, LocalDate to) {
        return transactionRepository.findByFilter(spaceId, userId, bankAccountId, categoryId, subCategoryId,
                        paymentMethodId, type, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUserId(), t.getBankAccountId(),
                t.getDestinationBankAccountId(), t.getCategoryId(), t.getSubCategoryId(), t.getPaymentMethodId(), t.getAmount(),
                t.getTransactionDate(), t.getDescription(), t.getCreatedDate());
    }
}
