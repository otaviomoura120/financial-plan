package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class MatchingInvoiceAmountsSpec extends Specification {

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User", null, null, null, null, "user@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private BankAccount buildAccount() {
        new BankAccount(1L, 0, buildSpace(), "Account", "BankCorp", BigDecimal.ZERO, true, Instant.now(), null)
    }

    private Transaction buildTransaction(BigDecimal amount, TransactionSourceType sourceType, Long sourceId) {
        Category category = new Category(10L, 0, null, "Category", true, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(20L, 0, null, "Cash", true, Instant.now(), null)
        new Transaction(1L, 0, TransactionType.EXPENSE, buildUser(), buildAccount(), null, category, null,
                paymentMethod, amount, LocalDate.now(), "desc", Instant.now(), null, sourceType, sourceId)
    }

    def "effectiveAmountFor returns the full transaction amount when there is no category filter"() {
        given:
        Transaction transaction = buildTransaction(new BigDecimal("450.00"), TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 5L)
        MatchingInvoiceAmounts matchingInvoiceAmounts = MatchingInvoiceAmounts.noFilter()

        expect:
        matchingInvoiceAmounts.effectiveAmountFor(transaction, LocalDate.of(2026, 3, 1)) == new BigDecimal("450.00")
    }

    def "effectiveAmountFor returns the full transaction amount when the transaction is not a credit card invoice payment"() {
        given:
        Transaction transaction = buildTransaction(new BigDecimal("450.00"), null, null)
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, LocalDate.of(2026, 3, 1))): new BigDecimal("120.00")])

        expect:
        matchingInvoiceAmounts.effectiveAmountFor(transaction, null) == new BigDecimal("450.00")
    }

    def "effectiveAmountFor returns the full transaction amount when referenceMonth is null"() {
        given:
        Transaction transaction = buildTransaction(new BigDecimal("450.00"), TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 5L)
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, LocalDate.of(2026, 3, 1))): new BigDecimal("120.00")])

        expect:
        matchingInvoiceAmounts.effectiveAmountFor(transaction, null) == new BigDecimal("450.00")
    }

    def "effectiveAmountFor returns the matched subtotal for the transaction's own invoice key"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction transaction = buildTransaction(new BigDecimal("450.00"), TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 5L)
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])

        expect:
        matchingInvoiceAmounts.effectiveAmountFor(transaction, referenceMonth) == new BigDecimal("120.00")
    }

    def "effectiveAmountFor returns ZERO when the transaction's invoice key has no matched subtotal"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction transaction = buildTransaction(new BigDecimal("450.00"), TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 5L)
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true, [:])

        expect:
        matchingInvoiceAmounts.effectiveAmountFor(transaction, referenceMonth) == BigDecimal.ZERO
    }
}
