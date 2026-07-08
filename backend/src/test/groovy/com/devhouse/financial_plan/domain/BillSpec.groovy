package com.devhouse.financial_plan.domain

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

    private Bill buildBill() {
        new Bill(10L, 0, buildSpace(), "Energy Bill", buildCategory(), new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    def "validate passes for a well-formed bill"() {
        given:
        Bill bill = buildBill()

        when:
        bill.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes when category is absent"() {
        given:
        Bill bill = new Bill(10L, 0, buildSpace(), "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)

        when:
        bill.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when name is blank"() {
        given:
        Bill bill = new Bill(10L, 0, buildSpace(), name, buildCategory(), new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)

        where:
        name << [null, "", "   "]
    }

    def "validate throws DomainException when space is null"() {
        given:
        Bill bill = new Bill(10L, 0, null, "Energy Bill", buildCategory(), new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when defaultAmount is null or not positive"() {
        given:
        Bill bill = new Bill(10L, 0, buildSpace(), "Energy Bill", buildCategory(), defaultAmount,
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)

        where:
        defaultAmount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "validate throws DomainException when startDate is null"() {
        given:
        Bill bill = new Bill(10L, 0, buildSpace(), "Energy Bill", buildCategory(), new BigDecimal("150.00"),
                null, true, true, Instant.now(), null)

        when:
        bill.validate()

        then:
        thrown(DomainException)
    }

    def "update replaces name, category and defaultAmount without touching the schedule"() {
        given:
        Bill bill = buildBill()
        Category newCategory = new Category(21L, 0, buildSpace(), "Housing", true, Instant.now(), null)

        when:
        bill.update("Rent", newCategory, new BigDecimal("2000.00"))

        then:
        bill.getName() == "Rent"
        bill.getCategory() == newCategory
        bill.getDefaultAmount() == new BigDecimal("2000.00")
        bill.getStartDate() == LocalDate.of(2026, 3, 10)
        bill.isRecurring()
    }

    def "updateSchedule replaces recurring and startDate without touching the basic fields"() {
        given:
        Bill bill = buildBill()

        when:
        bill.updateSchedule(false, LocalDate.of(2026, 6, 1))

        then:
        !bill.isRecurring()
        bill.getStartDate() == LocalDate.of(2026, 6, 1)
        bill.getName() == "Energy Bill"
        bill.getDefaultAmount() == new BigDecimal("150.00")
    }

    def "deactivate sets active to false"() {
        given:
        Bill bill = buildBill()

        when:
        bill.deactivate()

        then:
        !bill.isActive()
    }
}
