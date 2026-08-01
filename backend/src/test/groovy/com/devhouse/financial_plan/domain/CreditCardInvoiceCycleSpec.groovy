package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
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

    def "resolveReferenceMonth keeps the purchase in its own month when made before closing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, 10) == LocalDate.of(2026, 3, 1)

        where:
        purchaseDate << [LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9)]
    }

    def "resolveReferenceMonth pushes the purchase to the next month when made on or after closing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, 10) == LocalDate.of(2026, 4, 1)

        where:
        purchaseDate << [LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 31)]
    }

    def "resolveReferenceMonth clamps the closing date on a short month before comparing"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 2, 28), 31) == LocalDate.of(2026, 3, 1)
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 1), 31) == LocalDate.of(2026, 3, 1)
    }

    def "resolveReferenceMonth falls back to the computed month when no invoice is chosen"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 9), 10, null) == LocalDate.of(2026, 3, 1)
    }

    def "resolveReferenceMonth accepts the computed invoice as an explicit choice"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 10), 10, LocalDate.of(2026, 4, 1)) == LocalDate.of(2026, 4, 1)
    }

    def "resolveReferenceMonth accepts pushing the purchase to the next invoice"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 10), 10, LocalDate.of(2026, 5, 1)) == LocalDate.of(2026, 5, 1)
    }

    def "resolveReferenceMonth normalizes a chosen invoice that is not the first day of the month"() {
        expect:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 9), 10, LocalDate.of(2026, 3, 22)) == LocalDate.of(2026, 3, 1)
    }

    def "resolveReferenceMonth rejects an invoice that is neither the current nor the next one"() {
        when:
        CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 3, 9), 10, chosenReferenceMonth)

        then:
        DomainException exception = thrown(DomainException)
        exception.message == "Reference month must be the current or the next invoice"

        where:
        chosenReferenceMonth << [LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 1), LocalDate.of(2027, 3, 1)]
    }

    def "resolveDueDate falls in the month after the reference month when dueDay <= closingDay"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 3, 1), 10, 5) == LocalDate.of(2026, 4, 5)
    }

    def "resolveDueDate falls within the reference month when dueDay > closingDay"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 3, 1), 10, 17) == LocalDate.of(2026, 3, 17)
    }

    def "resolveReferenceMonth and resolveDueDate together reproduce the reported cycle: purchase on 2026-06-26 with closingDay 25 and dueDay 5 belongs to the invoice due 2026-08-05"() {
        given:
        int closingDay = 25
        int dueDay = 5

        when:
        LocalDate referenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(LocalDate.of(2026, 6, 26), closingDay)
        LocalDate dueDate = CreditCardInvoiceCycle.resolveDueDate(referenceMonth, closingDay, dueDay)

        then:
        referenceMonth == LocalDate.of(2026, 7, 1)
        dueDate == LocalDate.of(2026, 8, 5)
    }

    def "resolveDueDate clamps to the last day of a short due month"() {
        expect:
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 1, 1), 10, 5) == LocalDate.of(2026, 2, 5)
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 1, 1), 10, 30) == LocalDate.of(2026, 1, 30)
        CreditCardInvoiceCycle.resolveDueDate(YearMonth.of(2026, 1).atDay(1), 31, 31) == LocalDate.of(2026, 2, 28)
        CreditCardInvoiceCycle.resolveDueDate(LocalDate.of(2026, 2, 1), 5, 31) == LocalDate.of(2026, 2, 28)
    }
}
