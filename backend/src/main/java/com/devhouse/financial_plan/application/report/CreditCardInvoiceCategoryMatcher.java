package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CreditCardInvoiceCategoryMatcher {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;
    private final TransactionRepository transactionRepository;

    public CreditCardInvoiceCategoryMatcher(CreditCardTransactionRepository creditCardTransactionRepository,
                                             CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository,
                                             TransactionRepository transactionRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
        this.transactionRepository = transactionRepository;
    }

    public MatchingInvoiceAmounts resolveMatchingInvoiceAmounts(ReportFilterRequest filter) {
        if (!hasCategoryFilter(filter)) {
            return MatchingInvoiceAmounts.noFilter();
        }
        Map<MatchingInvoiceAmounts.InvoiceKey, BigDecimal> amounts = creditCardTransactionRepository.findByFilter(
                        filter.spaceId(), null, filter.categoryId(), filter.subCategoryId(), null, null, null).stream()
                .collect(Collectors.groupingBy(
                        item -> new MatchingInvoiceAmounts.InvoiceKey(item.getCreditCard().getId(), item.getReferenceMonth()),
                        Collectors.reducing(BigDecimal.ZERO, CreditCardTransaction::getAmount, BigDecimal::add)));
        return new MatchingInvoiceAmounts(true, amounts);
    }

    public List<Transaction> mergeWithMatchingInvoicePayments(List<Transaction> directMatches,
                                                                MatchingInvoiceAmounts matchingInvoiceAmounts,
                                                                ReportFilterRequest filter) {
        if (matchingInvoiceAmounts.isEmpty()) {
            return directMatches;
        }
        Set<Long> includedIds = directMatches.stream().map(Transaction::getId).collect(Collectors.toCollection(HashSet::new));
        List<Transaction> merged = new ArrayList<>(directMatches);
        for (MatchingInvoiceAmounts.InvoiceKey key : matchingInvoiceAmounts.keys()) {
            CreditCardInvoicePayment payment = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                    key.creditCardId(), key.referenceMonth());
            if (payment == null || includedIds.contains(payment.getPaymentTransactionId())) {
                continue;
            }
            Transaction transaction = transactionRepository.findById(payment.getPaymentTransactionId());
            if (transaction != null && matchesNonCategoryFilters(transaction, filter)) {
                merged.add(transaction);
                includedIds.add(transaction.getId());
            }
        }
        return merged;
    }

    public Map<Long, LocalDate> resolveInvoiceReferenceMonths(List<Transaction> transactions) {
        List<Long> invoicePaymentTransactionIds = transactions.stream()
                .filter(t -> TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT.equals(t.getSourceType()))
                .map(Transaction::getId)
                .toList();
        if (invoicePaymentTransactionIds.isEmpty()) {
            return Map.of();
        }
        return creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn(invoicePaymentTransactionIds).stream()
                .collect(Collectors.toMap(CreditCardInvoicePayment::getPaymentTransactionId, CreditCardInvoicePayment::getReferenceMonth));
    }

    private boolean hasCategoryFilter(ReportFilterRequest filter) {
        return filter.categoryId() != null || filter.subCategoryId() != null;
    }

    private boolean matchesNonCategoryFilters(Transaction transaction, ReportFilterRequest filter) {
        if (filter.userId() != null && !filter.userId().equals(transaction.getUser().getId())) {
            return false;
        }
        if (filter.bankAccountId() != null && !filter.bankAccountId().equals(transaction.getBankAccount().getId())) {
            return false;
        }
        if (filter.paymentMethodId() != null
                && (transaction.getPaymentMethod() == null || !filter.paymentMethodId().equals(transaction.getPaymentMethod().getId()))) {
            return false;
        }
        if (filter.type() != null && !filter.type().equals(transaction.getType())) {
            return false;
        }
        if (filter.from() != null && transaction.getTransactionDate().isBefore(filter.from())) {
            return false;
        }
        if (filter.to() != null && transaction.getTransactionDate().isAfter(filter.to())) {
            return false;
        }
        return true;
    }
}
