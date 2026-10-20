package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.CategoryKind;

/**
 * Bills, card purchases and card subscriptions are money going out, so they only accept expense categories. A credit
 * on the card invoice (refund, cashback) is money coming back, so it only accepts income categories.
 */
public final class CategoryKindPolicy {

    private CategoryKindPolicy() {
    }

    public static void requireExpense(Category category) {
        if (category == null) {
            return;
        }
        category.requireKind(CategoryKind.EXPENSE);
    }

    public static void requireForCreditCardEntry(Category category, boolean credit) {
        if (category == null) {
            return;
        }
        category.requireKind(credit ? CategoryKind.INCOME : CategoryKind.EXPENSE);
    }
}
