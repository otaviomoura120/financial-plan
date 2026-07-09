package com.devhouse.financial_plan.application.report;

import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record MatchingInvoiceAmounts(boolean hasCategoryFilter, Map<InvoiceKey, BigDecimal> amountsByInvoice) {

    public static MatchingInvoiceAmounts noFilter() {
        return new MatchingInvoiceAmounts(false, Map.of());
    }

    public boolean isEmpty() {
        return amountsByInvoice.isEmpty();
    }

    public Set<InvoiceKey> keys() {
        return amountsByInvoice.keySet();
    }

    public boolean excludesInvoice(InvoiceKey key) {
        return hasCategoryFilter && !amountsByInvoice.containsKey(key);
    }

    public BigDecimal amountOrDefault(InvoiceKey key, BigDecimal fallback) {
        return hasCategoryFilter ? amountsByInvoice.getOrDefault(key, BigDecimal.ZERO) : fallback;
    }

    public BigDecimal effectiveAmountFor(Transaction transaction, LocalDate referenceMonth) {
        if (!hasCategoryFilter || !TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT.equals(transaction.getSourceType())) {
            return transaction.getAmount();
        }
        if (referenceMonth == null) {
            return transaction.getAmount();
        }
        InvoiceKey key = new InvoiceKey(transaction.getSourceId(), referenceMonth);
        return amountsByInvoice.getOrDefault(key, BigDecimal.ZERO);
    }

    public record InvoiceKey(Long creditCardId, LocalDate referenceMonth) {}
}
