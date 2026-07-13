package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.application.report.dto.CategoryReportFilterRequest;
import com.devhouse.financial_plan.application.report.dto.CategoryReportGroupResponse;
import com.devhouse.financial_plan.application.report.dto.CategoryReportItemResponse;
import com.devhouse.financial_plan.application.report.dto.CategoryReportItemSource;
import com.devhouse.financial_plan.application.report.dto.CategoryReportResponse;
import com.devhouse.financial_plan.application.report.dto.CategoryReportSubGroupResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GenerateCategoryReportService {

    private final TransactionRepository transactionRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;

    public GenerateCategoryReportService(TransactionRepository transactionRepository,
                                         CreditCardTransactionRepository creditCardTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
    }

    public CategoryReportResponse execute(CategoryReportFilterRequest filter) {
        if (filter.spaceId() == null) {
            throw new DomainException("Space is required");
        }
        List<CategoryReportEntry> entries = new ArrayList<>();
        entries.addAll(transactionEntries(filter));
        entries.addAll(creditCardEntries(filter));
        sortEntries(entries);
        BigDecimal totalIncome = sumByType(entries, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(entries, TransactionType.EXPENSE);
        List<CategoryReportGroupResponse> groups = buildGroups(entries, totalIncome, totalExpense);
        return new CategoryReportResponse(totalIncome, totalExpense, totalIncome.subtract(totalExpense), groups);
    }

    private List<CategoryReportEntry> transactionEntries(CategoryReportFilterRequest filter) {
        if (filter.creditCardId() != null) {
            return List.of();
        }
        return transactionRepository.findByFilter(filter.spaceId(), filter.userId(), filter.bankAccountId(),
                        filter.categoryId(), filter.subCategoryId(), filter.type(),
                        filter.from(), filter.to()).stream()
                .filter(transaction -> !transaction.isTransfer())
                .filter(transaction -> !TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT.equals(transaction.getSourceType()))
                .map(this::toEntry)
                .toList();
    }

    private List<CategoryReportEntry> creditCardEntries(CategoryReportFilterRequest filter) {
        boolean typeAllows = filter.type() == null || TransactionType.EXPENSE.equals(filter.type());
        if (!typeAllows) {
            return List.of();
        }
        Map<String, BigDecimal> totalAmountByGroup = new HashMap<>();
        return creditCardTransactionRepository.findByFilter(filter.spaceId(), filter.creditCardId(), filter.categoryId(),
                        filter.subCategoryId(), filter.userId(), filter.from(), filter.to(), null).stream()
                .filter(purchase -> matchesBankAccount(purchase, filter.bankAccountId()))
                .map(purchase -> toEntry(purchase, resolveTotalAmount(purchase, totalAmountByGroup)))
                .toList();
    }

    private BigDecimal resolveTotalAmount(CreditCardTransaction transaction, Map<String, BigDecimal> cache) {
        if (transaction.getTotalInstallments() == null || transaction.getTotalInstallments() <= 1) {
            return transaction.getAmount();
        }
        return cache.computeIfAbsent(transaction.getInstallmentGroupId(), groupId ->
                creditCardTransactionRepository.findByInstallmentGroupId(groupId).stream()
                        .map(CreditCardTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private boolean matchesBankAccount(CreditCardTransaction purchase, Long bankAccountId) {
        if (bankAccountId == null) {
            return true;
        }
        BankAccount bankAccount = purchase.getCreditCard() != null ? purchase.getCreditCard().getBankAccount() : null;
        return bankAccount != null && bankAccountId.equals(bankAccount.getId());
    }

    private CategoryReportEntry toEntry(Transaction transaction) {
        CategoryReportItemResponse item = new CategoryReportItemResponse(transaction.getId(),
                CategoryReportItemSource.TRANSACTION, transaction.getType(), transaction.getTransactionDate(),
                transaction.getDescription(), transaction.getAmount(), transaction.getUser().getId(),
                transaction.getBankAccount().getId(),
                null, null, null, null, null, null);
        return new CategoryReportEntry(transaction.getCategory(), transaction.getSubCategory(), item);
    }

    private CategoryReportEntry toEntry(CreditCardTransaction purchase, BigDecimal totalAmount) {
        CreditCard creditCard = purchase.getCreditCard();
        BankAccount bankAccount = creditCard != null ? creditCard.getBankAccount() : null;
        CategoryReportItemResponse item = new CategoryReportItemResponse(purchase.getId(),
                CategoryReportItemSource.CREDIT_CARD, TransactionType.EXPENSE, purchase.getPurchaseDate(),
                purchase.getDescription(), purchase.getAmount(),
                purchase.getUser() != null ? purchase.getUser().getId() : null,
                bankAccount != null ? bankAccount.getId() : null,
                creditCard != null ? creditCard.getId() : null,
                creditCard != null ? creditCard.getName() : null,
                purchase.getInstallmentNumber(), purchase.getTotalInstallments(), totalAmount, purchase.getReferenceMonth());
        return new CategoryReportEntry(purchase.getCategory(), purchase.getSubCategory(), item);
    }

    private void sortEntries(List<CategoryReportEntry> entries) {
        Comparator<CategoryReportEntry> byCategory = Comparator.comparing(CategoryReportEntry::categoryName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        Comparator<CategoryReportEntry> bySubCategory = Comparator.comparing(CategoryReportEntry::subCategoryName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        Comparator<CategoryReportEntry> byDateDesc = Comparator.comparing(entry -> entry.item().date(),
                Comparator.reverseOrder());
        entries.sort(byCategory.thenComparing(bySubCategory).thenComparing(byDateDesc));
    }

    private List<CategoryReportGroupResponse> buildGroups(List<CategoryReportEntry> entries,
                                                          BigDecimal totalIncome, BigDecimal totalExpense) {
        Map<GroupKey, List<CategoryReportEntry>> byCategory = new LinkedHashMap<>();
        for (CategoryReportEntry entry : entries) {
            byCategory.computeIfAbsent(entry.categoryKey(), key -> new ArrayList<>()).add(entry);
        }
        List<CategoryReportGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<GroupKey, List<CategoryReportEntry>> group : byCategory.entrySet()) {
            groups.add(buildGroup(group.getKey(), group.getValue(), totalIncome, totalExpense));
        }
        return groups;
    }

    private CategoryReportGroupResponse buildGroup(GroupKey key, List<CategoryReportEntry> entries,
                                                   BigDecimal reportIncome, BigDecimal reportExpense) {
        BigDecimal income = sumByType(entries, TransactionType.INCOME);
        BigDecimal expense = sumByType(entries, TransactionType.EXPENSE);
        return new CategoryReportGroupResponse(key.id(), key.name(), income, expense, income.subtract(expense),
                percentage(income, reportIncome), percentage(expense, reportExpense), buildSubGroups(entries));
    }

    private List<CategoryReportSubGroupResponse> buildSubGroups(List<CategoryReportEntry> entries) {
        Map<GroupKey, List<CategoryReportEntry>> bySubCategory = new LinkedHashMap<>();
        for (CategoryReportEntry entry : entries) {
            bySubCategory.computeIfAbsent(entry.subCategoryKey(), key -> new ArrayList<>()).add(entry);
        }
        List<CategoryReportSubGroupResponse> subGroups = new ArrayList<>();
        for (Map.Entry<GroupKey, List<CategoryReportEntry>> subGroup : bySubCategory.entrySet()) {
            subGroups.add(buildSubGroup(subGroup.getKey(), subGroup.getValue()));
        }
        return subGroups;
    }

    private CategoryReportSubGroupResponse buildSubGroup(GroupKey key, List<CategoryReportEntry> entries) {
        BigDecimal income = sumByType(entries, TransactionType.INCOME);
        BigDecimal expense = sumByType(entries, TransactionType.EXPENSE);
        List<CategoryReportItemResponse> items = entries.stream()
                .map(CategoryReportEntry::item)
                .toList();
        return new CategoryReportSubGroupResponse(key.id(), key.name(), income, expense, income.subtract(expense), items);
    }

    private BigDecimal sumByType(List<CategoryReportEntry> entries, TransactionType type) {
        return entries.stream()
                .filter(entry -> type.equals(entry.item().type()))
                .map(entry -> entry.item().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private record GroupKey(Long id, String name) {}

    private record CategoryReportEntry(Category category, SubCategory subCategory, CategoryReportItemResponse item) {

        GroupKey categoryKey() {
            return new GroupKey(category != null ? category.getId() : null, categoryName());
        }

        GroupKey subCategoryKey() {
            return new GroupKey(subCategory != null ? subCategory.getId() : null, subCategoryName());
        }

        String categoryName() {
            return category != null ? category.getName() : null;
        }

        String subCategoryName() {
            return subCategory != null ? subCategory.getName() : null;
        }
    }
}
