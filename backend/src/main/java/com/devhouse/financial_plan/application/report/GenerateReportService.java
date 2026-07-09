package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.billinstance.EnsureRecurringBillsGeneratedService;
import com.devhouse.financial_plan.application.creditcardinvoice.ListCreditCardInvoicesService;
import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoiceResponse;
import com.devhouse.financial_plan.application.report.dto.PendingBillInstanceResponse;
import com.devhouse.financial_plan.application.report.dto.PendingCreditCardInvoiceResponse;
import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.ReportResponse;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class GenerateReportService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ListCreditCardInvoicesService listCreditCardInvoicesService;
    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;
    private final BillRepository billRepository;
    private final CreditCardInvoiceCategoryMatcher creditCardInvoiceCategoryMatcher;

    public GenerateReportService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                  ListCreditCardInvoicesService listCreditCardInvoicesService,
                                  EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService,
                                  BillRepository billRepository, CreditCardInvoiceCategoryMatcher creditCardInvoiceCategoryMatcher) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.listCreditCardInvoicesService = listCreditCardInvoicesService;
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
        this.billRepository = billRepository;
        this.creditCardInvoiceCategoryMatcher = creditCardInvoiceCategoryMatcher;
    }

    public ReportResponse execute(ReportFilterRequest filter) {
        if (filter.spaceId() == null) {
            throw new DomainException("Space is required");
        }

        List<Transaction> directMatches = transactionRepository.findByFilter(
                filter.spaceId(), filter.userId(), filter.bankAccountId(), filter.categoryId(), filter.subCategoryId(),
                filter.paymentMethodId(), filter.type(), filter.from(), filter.to());

        MatchingInvoiceAmounts matchingInvoiceAmounts = creditCardInvoiceCategoryMatcher.resolveMatchingInvoiceAmounts(filter);
        List<Transaction> transactions = matchingInvoiceAmounts.hasCategoryFilter()
                ? creditCardInvoiceCategoryMatcher.mergeWithMatchingInvoicePayments(directMatches, matchingInvoiceAmounts, filter)
                : directMatches;

        Map<Long, LocalDate> referenceMonthByTransactionId = creditCardInvoiceCategoryMatcher.resolveInvoiceReferenceMonths(transactions);

        List<TransactionResponse> responses = buildResponses(transactions, referenceMonthByTransactionId, matchingInvoiceAmounts);
        BigDecimal totalIncome = sumByType(transactions, referenceMonthByTransactionId, matchingInvoiceAmounts, true);
        BigDecimal totalExpense = sumByType(transactions, referenceMonthByTransactionId, matchingInvoiceAmounts, false);

        BigDecimal currentBalance = resolveCurrentBalance(filter);
        List<PendingCreditCardInvoiceResponse> pendingCreditCardInvoices = resolvePendingCreditCardInvoices(filter, matchingInvoiceAmounts);
        BigDecimal pendingCreditCardTotal = sum(pendingCreditCardInvoices, PendingCreditCardInvoiceResponse::amount);
        List<PendingBillInstanceResponse> pendingBillInstances = resolvePendingBillInstances(filter);
        BigDecimal pendingBillTotal = sum(pendingBillInstances, PendingBillInstanceResponse::amount);
        BigDecimal projectedBalance = currentBalance.subtract(pendingCreditCardTotal).subtract(pendingBillTotal);

        return new ReportResponse(responses, totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                currentBalance, pendingCreditCardInvoices, pendingCreditCardTotal, pendingBillInstances, pendingBillTotal,
                projectedBalance);
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

    private List<PendingCreditCardInvoiceResponse> resolvePendingCreditCardInvoices(ReportFilterRequest filter,
                                                                                      MatchingInvoiceAmounts matchingInvoiceAmounts) {
        List<PendingCreditCardInvoiceResponse> responses = new ArrayList<>();
        for (CreditCardInvoiceResponse invoice : listCreditCardInvoicesService.execute(filter.spaceId(), null, filter.from(), filter.to())) {
            if (invoice.paid()) {
                continue;
            }
            MatchingInvoiceAmounts.InvoiceKey key = new MatchingInvoiceAmounts.InvoiceKey(invoice.creditCardId(), invoice.referenceMonth());
            if (matchingInvoiceAmounts.excludesInvoice(key)) {
                continue;
            }
            BigDecimal amount = matchingInvoiceAmounts.amountOrDefault(key, invoice.totalAmount());
            responses.add(new PendingCreditCardInvoiceResponse(invoice.creditCardId(), invoice.creditCardName(),
                    invoice.referenceMonth(), invoice.dueDate(), amount));
        }
        return responses;
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

    private List<TransactionResponse> buildResponses(List<Transaction> transactions, Map<Long, LocalDate> referenceMonthByTransactionId,
                                                       MatchingInvoiceAmounts matchingInvoiceAmounts) {
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUser().getId(),
                        t.getBankAccount().getId(), t.getDestinationBankAccount() != null ? t.getDestinationBankAccount().getId() : null,
                        t.getCategory() != null ? t.getCategory().getId() : null,
                        t.getSubCategory() != null ? t.getSubCategory().getId() : null,
                        t.getPaymentMethod() != null ? t.getPaymentMethod().getId() : null,
                        matchingInvoiceAmounts.effectiveAmountFor(t, referenceMonthByTransactionId.get(t.getId())),
                        t.getTransactionDate(), t.getDescription(), t.getCreatedDate(), t.getSourceType(), t.getSourceId(),
                        referenceMonthByTransactionId.get(t.getId())))
                .toList();
    }

    private BigDecimal sumByType(List<Transaction> transactions, Map<Long, LocalDate> referenceMonthByTransactionId,
                                  MatchingInvoiceAmounts matchingInvoiceAmounts, boolean income) {
        return transactions.stream()
                .filter(t -> income ? t.isIncome() : t.isExpense())
                .map(t -> matchingInvoiceAmounts.effectiveAmountFor(t, referenceMonthByTransactionId.get(t.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
