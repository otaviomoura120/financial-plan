package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
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
        var transactions = transactionRepository.findByFilter(filter.userId(), filter.bankAccountId(), filter.categoryId(), filter.subCategoryId(), filter.paymentMethodId(), filter.type(), filter.from(), filter.to());

        var responses = transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getType(), t.getUserId(), t.getBankAccountId(), t.getCategoryId(), t.getSubCategoryId(), t.getPaymentMethodId(), t.getAmount(), t.getTransactionDate(), t.getDescription(), t.getCreatedDate()))
                .toList();

        var totalIncome = transactions.stream().filter(t -> t.isIncome()).map(t -> t.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalExpense = transactions.stream().filter(t -> t.isExpense()).map(t -> t.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportResponse(responses, totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }
}
