package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.exception.DomainException;

/**
 * System categories exist to label entries the application generates by itself (credit card invoice payments and
 * transfers). They must never be picked by hand on a user owned entry.
 */
public final class SystemCategoryPolicy {

    private SystemCategoryPolicy() {
    }

    public static void rejectSystemSelection(Category category, SubCategory subCategory) {
        if (category != null && category.isSystem()) {
            throw new DomainException("System categories cannot be selected manually");
        }
        if (subCategory != null && subCategory.isSystem()) {
            throw new DomainException("System subcategories cannot be selected manually");
        }
    }

    public static void rejectManualSelection(Category category, SubCategory subCategory, TransactionSourceType sourceType) {
        if (sourceType != null) {
            return;
        }
        rejectSystemSelection(category, subCategory);
    }
}
