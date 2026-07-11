package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class BillSpec extends Specification {

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private Category buildCategory() {
        new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
    }

    private SubCategory buildSubCategory() {
        new SubCategory(30L, 0, buildCategory(), "Electricity", true, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring() {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", buildCategory(), buildSubCategory(), new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private Bill buildInstance(BillInstanceStatus status, BillRecurring billRecurring = buildBillRecurring()) {
        new Bill(1L, 0, buildSpace(), billRecurring, "Energy Bill", buildCategory(), buildSubCategory(),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), status, null, null, null,
                false, Instant.now(), null)
    }

    def "validate passes for a well-formed pending bill"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING)

        when:
        bill.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes for a standalone bill with no billRecurring"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING, null)

        when:
        bill.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when space is null"() {
        given:
        Bill bill = new Bill(1L, 0, null, buildBillRecurring(), "Energy Bill", buildCategory(), buildSubCategory(),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING,
                null, null, null, false, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when name is blank"() {
        given:
        Bill bill = new Bill(1L, 0, buildSpace(), buildBillRecurring(), name, buildCategory(), buildSubCategory(),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING,
                null, null, null, false, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)

        where:
        name << [null, "", "   "]
    }

    def "validate throws DomainException when referenceMonth is null"() {
        given:
        Bill bill = new Bill(1L, 0, buildSpace(), buildBillRecurring(), "Energy Bill", buildCategory(), buildSubCategory(),
                null, LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING,
                null, null, null, false, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when dueDate is null"() {
        given:
        Bill bill = new Bill(1L, 0, buildSpace(), buildBillRecurring(), "Energy Bill", buildCategory(), buildSubCategory(),
                LocalDate.of(2026, 3, 1), null, new BigDecimal("150.00"), BillInstanceStatus.PENDING,
                null, null, null, false, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when amount is null or not positive"() {
        given:
        Bill bill = new Bill(1L, 0, buildSpace(), buildBillRecurring(), "Energy Bill", buildCategory(), buildSubCategory(),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), amount, BillInstanceStatus.PENDING,
                null, null, null, false, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)

        where:
        amount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "updateDetails replaces name, category, subCategory, amount and dueDate when pending"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING)
        Category newCategory = new Category(21L, 0, buildSpace(), "Housing", true, Instant.now(), null)
        SubCategory newSubCategory = new SubCategory(31L, 0, newCategory, "Rent", true, Instant.now(), null)

        when:
        bill.updateDetails("Rent", newCategory, newSubCategory, new BigDecimal("2000.00"), LocalDate.of(2026, 4, 5))

        then:
        bill.getName() == "Rent"
        bill.getCategory() == newCategory
        bill.getSubCategory() == newSubCategory
        bill.getAmount() == new BigDecimal("2000.00")
        bill.getDueDate() == LocalDate.of(2026, 4, 5)
    }

    def "updateDetails throws DomainException when already paid"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PAID)

        when:
        bill.updateDetails("Rent", null, null, new BigDecimal("2000.00"), LocalDate.of(2026, 4, 5))

        then:
        thrown(DomainException)
    }

    def "detachFromRecurring clears the billRecurring reference"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING)

        when:
        bill.detachFromRecurring()

        then:
        bill.getBillRecurring() == null
    }

    def "markAsPaid sets status to PAID and records the payment details when pending"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING)

        when:
        bill.markAsPaid(LocalDate.of(2026, 3, 9), 99L, 2L)

        then:
        bill.isPaid()
        bill.getPaidDate() == LocalDate.of(2026, 3, 9)
        bill.getPaymentTransactionId() == 99L
        bill.getBankAccountId() == 2L
    }

    def "markAsPaid throws DomainException when already paid"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PAID)

        when:
        bill.markAsPaid(LocalDate.of(2026, 3, 9), 99L, 2L)

        then:
        thrown(DomainException)
    }

    def "revertToPending resets status and clears payment details"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PAID)
        bill.setPaidDate(LocalDate.of(2026, 3, 9))
        bill.setPaymentTransactionId(99L)
        bill.setBankAccountId(2L)

        when:
        bill.revertToPending()

        then:
        bill.isPending()
        bill.getPaidDate() == null
        bill.getPaymentTransactionId() == null
        bill.getBankAccountId() == null
    }
}
