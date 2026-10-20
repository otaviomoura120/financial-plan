package com.devhouse.financial_plan.application.category;

import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.enums.CategoryKind;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A category can only switch between income and expense while nothing incompatible points to it. Otherwise the
 * existing entries would carry a category of the wrong type and could no longer be edited without changing it.
 */
@Service
public class CategoryKindChangeGuard {

    private final TransactionRepository transactionRepository;
    private final BillRepository billRepository;
    private final BillRecurringRepository billRecurringRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;

    public CategoryKindChangeGuard(TransactionRepository transactionRepository, BillRepository billRepository,
                                   BillRecurringRepository billRecurringRepository,
                                   CreditCardTransactionRepository creditCardTransactionRepository,
                                   CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository) {
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
        this.billRecurringRepository = billRecurringRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
    }

    public void rejectIncompatibleUsage(Category category, CategoryKind newKind) {
        if (!category.isKindChangedTo(newKind)) {
            return;
        }
        List<String> usages = CategoryKind.INCOME.equals(newKind)
                ? expenseUsages(category.getId())
                : incomeUsages(category.getId());
        if (!usages.isEmpty()) {
            throw new DomainException("Cannot change the category type: it is used by " + String.join(", ", usages)
                    + ". Move them to another category first.");
        }
    }

    private List<String> incomeUsages(Long categoryId) {
        List<String> usages = new ArrayList<>();
        addUsage(usages, transactionRepository.countByCategoryIdAndType(categoryId, TransactionType.INCOME),
                "income transaction(s)");
        addUsage(usages, creditCardTransactionRepository.countByCategoryIdAndCredit(categoryId, true),
                "credit card invoice credit(s)");
        return usages;
    }

    private List<String> expenseUsages(Long categoryId) {
        List<String> usages = new ArrayList<>();
        addUsage(usages, transactionRepository.countByCategoryIdAndType(categoryId, TransactionType.EXPENSE),
                "expense transaction(s)");
        addUsage(usages, billRepository.countByCategoryId(categoryId), "bill(s)");
        addUsage(usages, billRecurringRepository.countByCategoryId(categoryId), "recurring bill(s)");
        addUsage(usages, creditCardTransactionRepository.countByCategoryIdAndCredit(categoryId, false),
                "credit card transaction(s)");
        addUsage(usages, creditCardTransactionRecurringRepository.countByCategoryId(categoryId),
                "credit card subscription(s)");
        return usages;
    }

    private void addUsage(List<String> usages, long count, String label) {
        if (count > 0) {
            usages.add(count + " " + label);
        }
    }
}
