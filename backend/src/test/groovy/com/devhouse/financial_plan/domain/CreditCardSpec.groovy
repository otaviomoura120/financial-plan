package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class CreditCardSpec extends Specification {

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private CreditCard buildCreditCard() {
        new CreditCard(10L, 0, buildSpace(), null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    def "validate passes for a well-formed credit card"() {
        given:
        CreditCard creditCard = buildCreditCard()

        when:
        creditCard.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when name is blank"() {
        given:
        CreditCard creditCard = new CreditCard(10L, 0, buildSpace(), null, name, new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)

        when:
        creditCard.validate()

        then:
        thrown(DomainException)

        where:
        name << [null, "", "   "]
    }

    def "validate throws DomainException when space is null"() {
        given:
        CreditCard creditCard = new CreditCard(10L, 0, null, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)

        when:
        creditCard.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when limit is null or not positive"() {
        given:
        CreditCard creditCard = new CreditCard(10L, 0, buildSpace(), null, "Nubank", limit, 10, 17, true, Instant.now(), null)

        when:
        creditCard.validate()

        then:
        thrown(DomainException)

        where:
        limit << [null, BigDecimal.ZERO, new BigDecimal("-100.00")]
    }

    def "validate throws DomainException when closingDay is out of range"() {
        given:
        CreditCard creditCard = new CreditCard(10L, 0, buildSpace(), null, "Nubank", new BigDecimal("5000.00"), closingDay, 17, true, Instant.now(), null)

        when:
        creditCard.validate()

        then:
        thrown(DomainException)

        where:
        closingDay << [null, 0, 32]
    }

    def "validate throws DomainException when dueDay is out of range"() {
        given:
        CreditCard creditCard = new CreditCard(10L, 0, buildSpace(), null, "Nubank", new BigDecimal("5000.00"), 10, dueDay, true, Instant.now(), null)

        when:
        creditCard.validate()

        then:
        thrown(DomainException)

        where:
        dueDay << [null, 0, 32]
    }

    def "update replaces name, limit, closingDay, dueDay and bankAccount"() {
        given:
        CreditCard creditCard = buildCreditCard()
        BankAccount bankAccount = new BankAccount(5L, 0, buildSpace(), "Conta Corrente", "Itaú", BigDecimal.ZERO, true, Instant.now(), null)

        when:
        creditCard.update("Itaú", new BigDecimal("8000.00"), 5, 12, bankAccount)

        then:
        creditCard.getName() == "Itaú"
        creditCard.getLimit() == new BigDecimal("8000.00")
        creditCard.getClosingDay() == 5
        creditCard.getDueDay() == 12
        creditCard.getBankAccount().getId() == 5L
    }

    def "deactivate sets active to false"() {
        given:
        CreditCard creditCard = buildCreditCard()

        when:
        creditCard.deactivate()

        then:
        !creditCard.isActive()
    }
}
