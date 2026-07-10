package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreditCardInvoicePaymentSpec extends Specification {

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private CreditCardInvoicePayment buildPayment(CreditCard creditCard, LocalDate referenceMonth, LocalDate dueDate, BigDecimal paidAmount) {
        new CreditCardInvoicePayment(null, 0, creditCard, referenceMonth, dueDate, paidAmount,
                LocalDate.now(), 100L, 1L, Instant.now(), null)
    }

    def "validate passes for a well-formed invoice payment"() {
        given:
        CreditCardInvoicePayment payment = buildPayment(buildCreditCard(), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 5), new BigDecimal("350.00"))

        when:
        payment.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when credit card is null"() {
        given:
        CreditCardInvoicePayment payment = buildPayment(null, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 5), new BigDecimal("350.00"))

        when:
        payment.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when referenceMonth is null"() {
        given:
        CreditCardInvoicePayment payment = buildPayment(buildCreditCard(), null,
                LocalDate.of(2026, 4, 5), new BigDecimal("350.00"))

        when:
        payment.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when dueDate is null"() {
        given:
        CreditCardInvoicePayment payment = buildPayment(buildCreditCard(), LocalDate.of(2026, 3, 1),
                null, new BigDecimal("350.00"))

        when:
        payment.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when paidAmount is null or not positive"() {
        given:
        CreditCardInvoicePayment payment = buildPayment(buildCreditCard(), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 5), paidAmount)

        when:
        payment.validate()

        then:
        thrown(DomainException)

        where:
        paidAmount << [null, BigDecimal.ZERO, new BigDecimal("-50.00")]
    }
}
