package com.devhouse.financial_plan.domain.enums;

/**
 * Splits categories between the ones that label money coming in and the ones that label money going out. Transfers
 * have no kind: they move money between accounts and only use the reserved transfer category.
 */
public enum CategoryKind {
    INCOME, EXPENSE;

    public static CategoryKind of(TransactionType type) {
        if (TransactionType.INCOME.equals(type)) {
            return INCOME;
        }
        if (TransactionType.EXPENSE.equals(type)) {
            return EXPENSE;
        }
        return null;
    }
}
