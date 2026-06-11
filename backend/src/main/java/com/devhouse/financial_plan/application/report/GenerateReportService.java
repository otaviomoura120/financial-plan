package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GenerateReportService {

    private final TransactionRepository transactionRepository;

    public GenerateReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public ReportResponse execute(ReportFilterRequest filter) {
        List<Transaction> transactions = transactionRepository.findByFilter(
                filter.userId(), filter.bankAccountId(), filter.categoryId(), filter.subCategoryId(),
                filter.paymentMethodId(), filter.type(), filter.from(), filter.to());

        List<TransactionResponse> responses = buildResponses(transactions);
        BigDecimal totalIncome = sumByType(transactions, true);
        BigDecimal totalExpense = sumByType(transactions, false);

        return new ReportResponse(responses, totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }

    private List<TransactionResponse> buildResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getType(), t.getUserId(),
                        t.getBankAccountId(), t.getCategoryId(), t.getSubCategoryId(),
                        t.getPaymentMethodId(), t.getAmount(), t.getTransactionDate(),
                        t.getDescription(), t.getCreatedDate()))
                .toList();
    }

    private BigDecimal sumByType(List<Transaction> transactions, boolean income) {
        return transactions.stream()
                .filter(t -> income ? t.isIncome() : t.isExpense())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
