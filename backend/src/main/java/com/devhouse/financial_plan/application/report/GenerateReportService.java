package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.billinstance.EnsureRecurringBillsGeneratedService;
import com.devhouse.financial_plan.application.creditcardinvoice.ListCreditCardInvoicesService;
import com.devhouse.financial_plan.application.report.dto.PendingBillInstanceResponse;
import com.devhouse.financial_plan.application.report.dto.PendingCreditCardInvoiceResponse;
import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GenerateReportService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ListCreditCardInvoicesService listCreditCardInvoicesService;
    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;
    private final BillRepository billRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public GenerateReportService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                  ListCreditCardInvoicesService listCreditCardInvoicesService,
                                  EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService,
                                  BillRepository billRepository, CreditCardTransactionRepository creditCardTransactionRepository,
                                  CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.listCreditCardInvoicesService = listCreditCardInvoicesService;
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
        this.billRepository = billRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    public ReportResponse execute(ReportFilterRequest filter) {
        if (filter.spaceId() == null) {
            throw new DomainException("Space is required");
        }

        List<Transaction> directMatches = transactionRepository.findByFilter(
                filter.spaceId(), filter.userId(), filter.bankAccountId(), filter.categoryId(), filter.subCategoryId(),
                filter.paymentMethodId(), filter.type(), filter.from(), filter.to());

        boolean hasCategoryFilter = filter.categoryId() != null || filter.subCategoryId() != null;
        Set<InvoiceKey> matchingInvoiceKeys = hasCategoryFilter ? resolveMatchingInvoiceKeys(filter) : Set.of();
        List<Transaction> transactions = hasCategoryFilter
                ? mergeWithMatchingInvoicePayments(directMatches, matchingInvoiceKeys, filter)
                : directMatches;

        List<TransactionResponse> responses = buildResponses(transactions);
        BigDecimal totalIncome = sumByType(transactions, true);
        BigDecimal totalExpense = sumByType(transactions, false);

        BigDecimal currentBalance = resolveCurrentBalance(filter);
        List<PendingCreditCardInvoiceResponse> pendingCreditCardInvoices =
                resolvePendingCreditCardInvoices(filter, hasCategoryFilter, matchingInvoiceKeys);
        BigDecimal pendingCreditCardTotal = sum(pendingCreditCardInvoices, PendingCreditCardInvoiceResponse::amount);
        List<PendingBillInstanceResponse> pendingBillInstances = resolvePendingBillInstances(filter);
        BigDecimal pendingBillTotal = sum(pendingBillInstances, PendingBillInstanceResponse::amount);
        BigDecimal projectedBalance = currentBalance.subtract(pendingCreditCardTotal).subtract(pendingBillTotal);

        return new ReportResponse(responses, totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                currentBalance, pendingCreditCardInvoices, pendingCreditCardTotal, pendingBillInstances, pendingBillTotal,
                projectedBalance);
    }

    private Set<InvoiceKey> resolveMatchingInvoiceKeys(ReportFilterRequest filter) {
        return creditCardTransactionRepository.findByFilter(filter.spaceId(), null, filter.categoryId(), filter.subCategoryId(),
                        filter.from(), filter.to(), null).stream()
                .map(item -> new InvoiceKey(item.getCreditCard().getId(), item.getReferenceMonth()))
                .collect(Collectors.toSet());
    }

    private List<Transaction> mergeWithMatchingInvoicePayments(List<Transaction> directMatches, Set<InvoiceKey> matchingInvoiceKeys,
                                                                 ReportFilterRequest filter) {
        if (matchingInvoiceKeys.isEmpty()) {
            return directMatches;
        }
        Set<Long> includedIds = directMatches.stream().map(Transaction::getId).collect(Collectors.toCollection(HashSet::new));
        List<Transaction> merged = new ArrayList<>(directMatches);
        for (InvoiceKey key : matchingInvoiceKeys) {
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

    private BigDecimal resolveCurrentBalance(ReportFilterRequest filter) {
        if (filter.bankAccountId() != null) {
            BankAccount account = bankAccountRepository.findById(filter.bankAccountId());
            return account != null ? account.getBalance() : BigDecimal.ZERO;
        }
        return bankAccountRepository.findBySpaceId(filter.spaceId()).stream()
                .filter(BankAccount::isActive)
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PendingCreditCardInvoiceResponse> resolvePendingCreditCardInvoices(ReportFilterRequest filter, boolean hasCategoryFilter,
                                                                                      Set<InvoiceKey> matchingInvoiceKeys) {
        return listCreditCardInvoicesService.execute(filter.spaceId(), null, filter.from(), filter.to()).stream()
                .filter(invoice -> !invoice.paid())
                .filter(invoice -> !hasCategoryFilter
                        || matchingInvoiceKeys.contains(new InvoiceKey(invoice.creditCardId(), invoice.referenceMonth())))
                .map(invoice -> new PendingCreditCardInvoiceResponse(invoice.creditCardId(), invoice.creditCardName(),
                        invoice.referenceMonth(), invoice.dueDate(), invoice.totalAmount()))
                .toList();
    }

    private List<PendingBillInstanceResponse> resolvePendingBillInstances(ReportFilterRequest filter) {
        LocalDate upToDate = filter.to() != null ? filter.to() : LocalDate.now();
        ensureRecurringBillsGeneratedService.execute(filter.spaceId(), upToDate);
        return billRepository.findBySpaceAndPeriod(filter.spaceId(), filter.from(), filter.to(), filter.categoryId(), filter.subCategoryId()).stream()
                .filter(Bill::isPending)
                .map(bill -> new PendingBillInstanceResponse(bill.getId(),
                        bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null,
                        bill.getName(), bill.getReferenceMonth(), bill.getDueDate(), bill.getAmount(),
                        bill.getCategory() != null ? bill.getCategory().getId() : null,
                        bill.getSubCategory() != null ? bill.getSubCategory().getId() : null))
                .toList();
    }

    private <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> amountExtractor) {
        return items.stream()
                .map(amountExtractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<TransactionResponse> buildResponses(List<Transaction> transactions) {
        Map<Long, LocalDate> referenceMonthByTransactionId = resolveInvoiceReferenceMonths(transactions);
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUser().getId(),
                        t.getBankAccount().getId(), t.getDestinationBankAccount() != null ? t.getDestinationBankAccount().getId() : null,
                        t.getCategory() != null ? t.getCategory().getId() : null,
                        t.getSubCategory() != null ? t.getSubCategory().getId() : null,
                        t.getPaymentMethod() != null ? t.getPaymentMethod().getId() : null, t.getAmount(),
                        t.getTransactionDate(), t.getDescription(), t.getCreatedDate(), t.getSourceType(), t.getSourceId(),
                        referenceMonthByTransactionId.get(t.getId())))
                .toList();
    }

    private Map<Long, LocalDate> resolveInvoiceReferenceMonths(List<Transaction> transactions) {
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

    private BigDecimal sumByType(List<Transaction> transactions, boolean income) {
        return transactions.stream()
                .filter(t -> income ? t.isIncome() : t.isExpense())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record InvoiceKey(Long creditCardId, LocalDate referenceMonth) {}
}
