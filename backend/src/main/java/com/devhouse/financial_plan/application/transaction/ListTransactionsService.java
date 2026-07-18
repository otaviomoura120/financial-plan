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
                                              Long subCategoryId, TransactionType type,
                                              LocalDate from, LocalDate to) {
        return transactionRepository.findByFilter(spaceId, userId, bankAccountId, categoryId, subCategoryId,
                        type, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUser().getId(), t.getBankAccount().getId(),
                t.getDestinationBankAccount() != null ? t.getDestinationBankAccount().getId() : null,
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(),
                t.getTransactionDate(), t.getDescription(), t.getCreatedDate(), t.getSourceType(), t.getSourceId(), null);
    }
}
