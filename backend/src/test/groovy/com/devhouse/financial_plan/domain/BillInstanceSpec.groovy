package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class BillInstanceSpec extends Specification {

    private Bill buildBill() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    private BillInstance buildInstance(BillInstanceStatus status) {
        new BillInstance(1L, 0, buildBill(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                new BigDecimal("150.00"), status, null, null, null, Instant.now(), null)
    }

    def "validate passes for a well-formed pending instance"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PENDING)

        when:
        instance.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when bill is null"() {
        given:
        BillInstance instance = new BillInstance(1L, 0, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)

        when:
        instance.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when referenceMonth is null"() {
        given:
        BillInstance instance = new BillInstance(1L, 0, buildBill(), null, LocalDate.of(2026, 3, 10),
                new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)

        when:
        instance.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when dueDate is null"() {
        given:
        BillInstance instance = new BillInstance(1L, 0, buildBill(), LocalDate.of(2026, 3, 1), null,
                new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)

        when:
        instance.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when amount is null or not positive"() {
        given:
        BillInstance instance = new BillInstance(1L, 0, buildBill(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                amount, BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)

        when:
        instance.validate()

        then:
        thrown(DomainException)

        where:
        amount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "updateAmount replaces the amount when pending"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PENDING)

        when:
        instance.updateAmount(new BigDecimal("200.00"))

        then:
        instance.getAmount() == new BigDecimal("200.00")
    }

    def "updateAmount throws DomainException when already paid"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PAID)

        when:
        instance.updateAmount(new BigDecimal("200.00"))

        then:
        thrown(DomainException)
    }

    def "markAsPaid sets status to PAID and records the payment details when pending"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PENDING)

        when:
        instance.markAsPaid(LocalDate.of(2026, 3, 9), 99L, 2L)

        then:
        instance.isPaid()
        instance.getPaidDate() == LocalDate.of(2026, 3, 9)
        instance.getPaymentTransactionId() == 99L
        instance.getBankAccountId() == 2L
    }

    def "markAsPaid throws DomainException when already paid"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PAID)

        when:
        instance.markAsPaid(LocalDate.of(2026, 3, 9), 99L, 2L)

        then:
        thrown(DomainException)
    }

    def "revertToPending resets status and clears payment details"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PAID)
        instance.setPaidDate(LocalDate.of(2026, 3, 9))
        instance.setPaymentTransactionId(99L)
        instance.setBankAccountId(2L)

        when:
        instance.revertToPending()

        then:
        instance.isPending()
        instance.getPaidDate() == null
        instance.getPaymentTransactionId() == null
        instance.getBankAccountId() == null
    }
}
