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
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Service
public class GenerateReportService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ListCreditCardInvoicesService listCreditCardInvoicesService;
    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;
    private final BillRepository billRepository;

    public GenerateReportService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                  ListCreditCardInvoicesService listCreditCardInvoicesService,
                                  EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService,
                                  BillRepository billRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.listCreditCardInvoicesService = listCreditCardInvoicesService;
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
        this.billRepository = billRepository;
    }

    public ReportResponse execute(ReportFilterRequest filter) {
        if (filter.spaceId() == null) {
            throw new DomainException("Space is required");
        }
        List<Transaction> transactions = transactionRepository.findByFilter(
                filter.spaceId(), filter.userId(), filter.bankAccountId(), filter.categoryId(), filter.subCategoryId(),
                filter.paymentMethodId(), filter.type(), filter.from(), filter.to());

        List<TransactionResponse> responses = buildResponses(transactions);
        BigDecimal totalIncome = sumByType(transactions, true);
        BigDecimal totalExpense = sumByType(transactions, false);

        BigDecimal currentBalance = resolveCurrentBalance(filter);
        List<PendingCreditCardInvoiceResponse> pendingCreditCardInvoices = resolvePendingCreditCardInvoices(filter);
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

    private List<PendingCreditCardInvoiceResponse> resolvePendingCreditCardInvoices(ReportFilterRequest filter) {
        return listCreditCardInvoicesService.execute(filter.spaceId(), null, filter.from(), filter.to()).stream()
                .filter(invoice -> !invoice.paid())
                .map(invoice -> new PendingCreditCardInvoiceResponse(invoice.creditCardId(), invoice.creditCardName(),
                        invoice.referenceMonth(), invoice.dueDate(), invoice.totalAmount()))
                .toList();
    }

    private List<PendingBillInstanceResponse> resolvePendingBillInstances(ReportFilterRequest filter) {
        LocalDate upToDate = filter.to() != null ? filter.to() : LocalDate.now();
        ensureRecurringBillsGeneratedService.execute(filter.spaceId(), upToDate);
        return billRepository.findBySpaceAndPeriod(filter.spaceId(), filter.from(), filter.to()).stream()
                .filter(Bill::isPending)
                .map(bill -> new PendingBillInstanceResponse(bill.getId(),
                        bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null,
                        bill.getName(), bill.getReferenceMonth(), bill.getDueDate(), bill.getAmount()))
                .toList();
    }

    private <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> amountExtractor) {
        return items.stream()
                .map(amountExtractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<TransactionResponse> buildResponses(List<Transaction> transactions) {
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUser().getId(),
                        t.getBankAccount().getId(), t.getDestinationBankAccount() != null ? t.getDestinationBankAccount().getId() : null,
                        t.getCategory() != null ? t.getCategory().getId() : null,
                        t.getSubCategory() != null ? t.getSubCategory().getId() : null,
                        t.getPaymentMethod() != null ? t.getPaymentMethod().getId() : null, t.getAmount(),
                        t.getTransactionDate(), t.getDescription(), t.getCreatedDate()))
                .toList();
    }

    private BigDecimal sumByType(List<Transaction> transactions, boolean income) {
        return transactions.stream()
                .filter(t -> income ? t.isIncome() : t.isExpense())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
