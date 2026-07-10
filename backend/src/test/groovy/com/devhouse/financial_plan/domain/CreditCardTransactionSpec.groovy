package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreditCardTransactionSpec extends Specification {

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category buildCategory() {
        new Category(20L, 0, null, "Food", true, Instant.now(), null)
    }

    private CreditCardTransaction buildTransaction(String installmentGroupId, Integer installmentNumber, Integer totalInstallments) {
        new CreditCardTransaction(null, 0, buildCreditCard(), buildUser(), buildCategory(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                installmentGroupId, installmentNumber, totalInstallments, false, null, Instant.now(), null)
    }

    def "validate passes for a well-formed single-installment purchase"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 1, 1)

        when:
        transaction.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes for a well-formed installment within a group"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 3, 12)

        when:
        transaction.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when creditCard is null"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, null, buildUser(), buildCategory(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when user is null"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, buildCreditCard(), null, buildCategory(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when category is null"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, buildCreditCard(), buildUser(), null, null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when amount is null or not positive"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, buildCreditCard(), buildUser(), buildCategory(), null,
                amount, LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        amount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "validate throws DomainException when purchaseDate is null"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, buildCreditCard(), buildUser(), buildCategory(), null,
                new BigDecimal("100.00"), null, "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when referenceMonth is null"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, buildCreditCard(), buildUser(), buildCategory(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", null,
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        transaction.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when installmentGroupId is null or blank"() {
        given:
        CreditCardTransaction transaction = buildTransaction(installmentGroupId, 1, 1)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        installmentGroupId << [null, "", "   "]
    }

    def "validate throws DomainException when totalInstallments is out of range"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 1, totalInstallments)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        totalInstallments << [null, 0, 61]
    }

    def "validate throws DomainException when installmentNumber is out of range"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", installmentNumber, 12)

        when:
        transaction.validate()

        then:
        thrown(DomainException)

        where:
        installmentNumber << [null, 0, 13]
    }

    def "anticipateTo moves the reference month and records the original one on first anticipation"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 10, 12)

        when:
        transaction.anticipateTo(LocalDate.of(2026, 6, 1))

        then:
        transaction.getReferenceMonth() == LocalDate.of(2026, 6, 1)
        transaction.isAnticipated()
        transaction.getOriginalReferenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "anticipateTo preserves the original reference month across repeated anticipations"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 10, 12)
        transaction.anticipateTo(LocalDate.of(2026, 6, 1))

        when:
        transaction.anticipateTo(LocalDate.of(2026, 5, 1))

        then:
        transaction.getReferenceMonth() == LocalDate.of(2026, 5, 1)
        transaction.isAnticipated()
        transaction.getOriginalReferenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "update replaces category, subCategory, amount, purchaseDate and description without touching installment fields"() {
        given:
        CreditCardTransaction transaction = buildTransaction("group-1", 3, 12)
        Category newCategory = new Category(21L, 0, null, "Travel", true, Instant.now(), null)
        SubCategory newSubCategory = new SubCategory(30L, 0, newCategory, "Flights", true, null, null)

        when:
        transaction.update(newCategory, newSubCategory, new BigDecimal("250.00"), LocalDate.of(2026, 3, 20), "new desc")

        then:
        transaction.getCategory() == newCategory
        transaction.getSubCategory() == newSubCategory
        transaction.getAmount() == new BigDecimal("250.00")
        transaction.getPurchaseDate() == LocalDate.of(2026, 3, 20)
        transaction.getDescription() == "new desc"
        transaction.getReferenceMonth() == LocalDate.of(2026, 3, 1)
        transaction.getInstallmentNumber() == 3
        transaction.getTotalInstallments() == 12
    }
}
