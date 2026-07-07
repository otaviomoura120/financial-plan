package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class BankAccountSpec extends Specification {

    private BankAccount buildAccount(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(10L, 0, space, "Main Account", "BankCorp", new BigDecimal("500.00"), active, Instant.now(), null)
    }

    def "deactivate sets active to false"() {
        given:
        BankAccount account = buildAccount(true)

        when:
        account.deactivate()

        then:
        !account.isActive()
    }

    def "activate sets active to true"() {
        given:
        BankAccount account = buildAccount(false)

        when:
        account.activate()

        then:
        account.isActive()
    }

    def "credit increases the balance"() {
        given:
        BankAccount account = buildAccount(true)

        when:
        account.credit(new BigDecimal("100.00"))

        then:
        account.getBalance() == new BigDecimal("600.00")
    }

    def "debit decreases the balance, even into negative territory"() {
        given:
        BankAccount account = buildAccount(true)

        when:
        account.debit(new BigDecimal("600.00"))

        then:
        account.getBalance() == new BigDecimal("-100.00")
    }

    def "credit throws DomainException for a non-positive amount"() {
        given:
        BankAccount account = buildAccount(true)

        when:
        account.credit(amount)

        then:
        thrown(DomainException)

        where:
        amount << [BigDecimal.ZERO, new BigDecimal("-10.00"), null]
    }
}
