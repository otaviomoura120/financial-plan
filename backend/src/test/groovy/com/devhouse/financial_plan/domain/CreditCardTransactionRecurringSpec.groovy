package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreditCardTransactionRecurringSpec extends Specification {

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category buildCategory() {
        new Category(20L, 0, null, "Assinaturas", true, Instant.now(), null)
    }

    private SubCategory buildSubCategory() {
        new SubCategory(30L, 0, buildCategory(), "Streaming", true, null, null)
    }

    private CreditCardTransactionRecurring buildRecurring() {
        new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(), buildCategory(), buildSubCategory(),
                "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "validate passes for a well-formed recurring"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()

        when:
        recurring.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes when subCategory and description are absent"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(),
                buildCategory(), null, null, new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when creditCard is null"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, null, buildUser(),
                buildCategory(), null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when user is null"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), null,
                buildCategory(), null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when category is null"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(),
                null, null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when defaultAmount is null or not positive"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(),
                buildCategory(), null, "Netflix", defaultAmount, LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        thrown(DomainException)

        where:
        defaultAmount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "validate throws DomainException when startDate is null"() {
        given:
        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(),
                buildCategory(), null, "Netflix", new BigDecimal("39.90"), null, true, Instant.now(), null)

        when:
        recurring.validate()

        then:
        thrown(DomainException)
    }

    def "update replaces category, subCategory, defaultAmount and description without touching the schedule"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()
        Category newCategory = new Category(21L, 0, null, "Lazer", true, Instant.now(), null)
        SubCategory newSubCategory = new SubCategory(31L, 0, newCategory, "Jogos", true, null, null)

        when:
        recurring.update(newCategory, newSubCategory, new BigDecimal("59.90"), "Xbox Game Pass")

        then:
        recurring.getCategory() == newCategory
        recurring.getSubCategory() == newSubCategory
        recurring.getDefaultAmount() == new BigDecimal("59.90")
        recurring.getDescription() == "Xbox Game Pass"
        recurring.getStartDate() == LocalDate.of(2026, 3, 10)
    }

    def "updateSchedule replaces startDate without touching the other fields"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()

        when:
        recurring.updateSchedule(LocalDate.of(2026, 6, 1))

        then:
        recurring.getStartDate() == LocalDate.of(2026, 6, 1)
        recurring.getDescription() == "Netflix"
        recurring.getDefaultAmount() == new BigDecimal("39.90")
    }

    def "deactivate sets active to false"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()

        when:
        recurring.deactivate()

        then:
        !recurring.isActive()
    }

    def "setVersion throws ObjectOptimisticLockingFailureException when versions diverge"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()

        when:
        recurring.setVersion(99)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
    }
}
