package com.devhouse.financial_plan.domain;

import java.time.LocalDate;
import java.time.YearMonth;

public final class CreditCardInvoiceCycle {

    private CreditCardInvoiceCycle() {
    }

    public static LocalDate resolveClosingDate(YearMonth month, int closingDay) {
        int day = Math.min(closingDay, month.lengthOfMonth());
        return month.atDay(day);
    }

    public static LocalDate resolveReferenceMonth(LocalDate purchaseDate, int closingDay) {
        YearMonth purchaseMonth = YearMonth.from(purchaseDate);
        LocalDate closingDate = resolveClosingDate(purchaseMonth, closingDay);
        YearMonth referenceMonth = purchaseDate.isBefore(closingDate) ? purchaseMonth : purchaseMonth.plusMonths(1);
        return referenceMonth.atDay(1);
    }

    public static LocalDate resolveDueDate(LocalDate referenceMonth, int closingDay, int dueDay) {
        YearMonth referenceYearMonth = YearMonth.from(referenceMonth);
        YearMonth dueMonth = dueDay <= closingDay ? referenceYearMonth.plusMonths(1) : referenceYearMonth;
        int day = Math.min(dueDay, dueMonth.lengthOfMonth());
        return dueMonth.atDay(day);
    }
}
