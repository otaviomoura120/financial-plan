package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class TransactionSpec extends Specification {

    private User buildUser(Long id) {
        new User(id, 0, "auth0|" + id, "User " + id, null, null, null, null, "user${id}@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private BankAccount buildBankAccount(Long id) {
        id == null ? null : new BankAccount(id, 0, null, "Account " + id, null, BigDecimal.ZERO, true, Instant.now(), null)
    }

    private Category buildCategory(Long id) {
        id == null ? null : new Category(id, 0, null, "Category " + id, true, Instant.now(), null)
    }

    private PaymentMethod buildPaymentMethod(Long id) {
        id == null ? null : new PaymentMethod(id, 0, null, "Method " + id, true, Instant.now(), null)
    }

    private Transaction buildTransaction(TransactionType type, Long bankAccountId, Long destinationBankAccountId,
                                          Long categoryId, Long paymentMethodId) {
        new Transaction(null, 0, type, buildUser(1L), buildBankAccount(bankAccountId), buildBankAccount(destinationBankAccountId),
                buildCategory(categoryId), null, buildPaymentMethod(paymentMethodId), new BigDecimal("100.00"),
                LocalDate.now(), "desc", Instant.now(), null, null, null)
    }

    def "validate passes for INCOME with category and payment method"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.INCOME, 1L, null, 10L, 20L)

        when:
        transaction.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes for EXPENSE with category and payment method"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.EXPENSE, 1L, null, 10L, 20L)

        when:
        transaction.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when INCOME/EXPENSE is missing category"() {
        given:
        Transaction transaction = buildTransaction(type, 1L, null, null, 20L)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        type << [TransactionType.INCOME, TransactionType.EXPENSE]
    }

    def "validate throws DomainException when INCOME/EXPENSE is missing payment method"() {
        given:
        Transaction transaction = buildTransaction(type, 1L, null, 10L, null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        type << [TransactionType.INCOME, TransactionType.EXPENSE]
    }

    def "validate passes for TRANSFER without category/payment method when destination differs from origin"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.TRANSFER, 1L, 2L, null, null)

        when:
        transaction.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException for TRANSFER without destination bank account"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.TRANSFER, 1L, null, null, null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException for TRANSFER when destination equals origin"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.TRANSFER, 1L, 1L, null, null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "isIncome/isExpense/isTransfer reflect the transaction type"() {
        given:
        Transaction income = buildTransaction(TransactionType.INCOME, 1L, null, 10L, 20L)
        Transaction expense = buildTransaction(TransactionType.EXPENSE, 1L, null, 10L, 20L)
        Transaction transfer = buildTransaction(TransactionType.TRANSFER, 1L, 2L, null, null)

        expect:
        income.isIncome() && !income.isExpense() && !income.isTransfer()
        expense.isExpense() && !expense.isIncome() && !expense.isTransfer()
        transfer.isTransfer() && !transfer.isIncome() && !transfer.isExpense()
    }

    def "isLinkedToSource is false when sourceType is not set"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.EXPENSE, 1L, null, 10L, 20L)

        expect:
        !transaction.isLinkedToSource()
    }

    def "isLinkedToSource is true when sourceType is set"() {
        given:
        Transaction transaction = new Transaction(1L, 0, TransactionType.EXPENSE, buildUser(1L), buildBankAccount(1L), null,
                buildCategory(10L), null, buildPaymentMethod(20L), new BigDecimal("100.00"), LocalDate.now(), "desc",
                Instant.now(), null, TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 99L)

        expect:
        transaction.isLinkedToSource()
        transaction.getSourceType() == TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT
        transaction.getSourceId() == 99L
    }
}
