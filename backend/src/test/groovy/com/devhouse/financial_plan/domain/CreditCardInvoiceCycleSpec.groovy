package com.devhouse.financial_plan.domain

import spock.lang.Specification

import java.time.LocalDate
import java.time.YearMonth

class CreditCardInvoiceCycleSpec extends Specification {

    def "resolveClosingDate returns the closing day within the given month"() {
        expect:
        CreditCardInvoiceCycle.resolveClosingDate(YearMonth.of(2026, 3), 10) == LocalDate.of(2026, 3, 10)
    }

    def "resolveClosingDate clamps to the last day of a short month"() {
        expect:
        CreditCardInvoiceCycle.resolveClosingDate(YearMonth.of(2026, 2), 31) == LocalDate.of(2026, 2, 28)
    }

    def "resolveClosingDate clamps to the last day of February in a leap year"() {
        expect:
        CreditCardInvoiceCycle.resolveClosingDate(YearMonth.of(2028, 2), 31) == LocalDate.of(2028, 2, 29)
    }

    def "resolveReferenceMonth keeps the purchase in its own month when made on or before closing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, 10) == LocalDate.of(2026, 3, 1)

        where:
        purchaseDate << [LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 10)]
    }

    def "resolveReferenceMonth pushes the purchase to the next month when made after closing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, 10) == LocalDate.of(2026, 4, 1)

        where:
        purchaseDate << [LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 31)]
    }

    def "resolveReferenceMonth clamps the closing date on a short month before comparing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 2, 28), 31) == LocalDate.of(2026, 2, 1)
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 1), 31) == LocalDate.of(2026, 3, 1)
    }

    def "resolveDueDate falls in the month after the reference month when dueDay <= closingDay"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 3, 1), 10, 5) == LocalDate.of(2026, 4, 5)
    }

    def "resolveDueDate falls within the reference month when dueDay > closingDay"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 3, 1), 10, 17) == LocalDate.of(2026, 3, 17)
    }

    def "resolveDueDate clamps to the last day of a short due month"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 1, 1), 10, 5) == LocalDate.of(2026, 2, 5)
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 1, 1), 10, 30) == LocalDate.of(2026, 1, 30)
        CreditCardInvoiceCycle.resolveDueDate(YearMonth.of(2026, 1).atDay(1), 31, 31) == LocalDate.of(2026, 2, 28)
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 2, 1), 5, 31) == LocalDate.of(2026, 2, 28)
    }
}
