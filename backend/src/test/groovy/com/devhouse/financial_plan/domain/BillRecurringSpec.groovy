package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class BillRecurringSpec extends Specification {

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
                LocalDate.of(2026, 3, 10), null, null, true, Instant.now(), null)
    }

    def "validate passes for a well-formed bill recurring"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()

        when:
        billRecurring.validate()

        then:
        noExceptionThrown()
    }

    def "validate passes when category and subCategory are absent"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), null, null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when name is blank"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), name, buildCategory(), buildSubCategory(),
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), null, null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)

        where:
        name << [null, "", "   "]
    }

    def "validate throws DomainException when space is null"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, null, "Energy Bill", buildCategory(), buildSubCategory(),
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), null, null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when defaultAmount is null or not positive"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", buildCategory(), buildSubCategory(),
                defaultAmount, LocalDate.of(2026, 3, 10), null, null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)

        where:
        defaultAmount << [null, BigDecimal.ZERO, new BigDecimal("-10.00")]
    }

    def "validate throws DomainException when startDate is null"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", buildCategory(), buildSubCategory(),
                new BigDecimal("150.00"), null, null, null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)
    }

    def "update replaces name, category, subCategory and defaultAmount without touching the schedule"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()
        Category newCategory = new Category(21L, 0, buildSpace(), "Housing", true, Instant.now(), null)
        SubCategory newSubCategory = new SubCategory(31L, 0, newCategory, "Rent", true, Instant.now(), null)

        when:
        billRecurring.update("Rent", newCategory, newSubCategory, new BigDecimal("2000.00"))

        then:
        billRecurring.getName() == "Rent"
        billRecurring.getCategory() == newCategory
        billRecurring.getSubCategory() == newSubCategory
        billRecurring.getDefaultAmount() == new BigDecimal("2000.00")
        billRecurring.getStartDate() == LocalDate.of(2026, 3, 10)
    }

    def "updateSchedule replaces startDate and the recurrence end without touching the basic fields"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()

        when:
        billRecurring.updateSchedule(LocalDate.of(2026, 6, 1), null, 8)

        then:
        billRecurring.getStartDate() == LocalDate.of(2026, 6, 1)
        billRecurring.getInstallments() == 8
        billRecurring.getName() == "Energy Bill"
        billRecurring.getDefaultAmount() == new BigDecimal("150.00")
    }

    def "lastReferenceMonth is null when the recurrence never ends"() {
        expect:
        buildBillRecurring().lastReferenceMonth() == null
    }

    def "lastReferenceMonth derives the closing month from the installments count"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), null, 3, true, Instant.now(), null)

        expect:
        billRecurring.lastReferenceMonth() == YearMonth.of(2026, 5)
        billRecurring.isFinishedOn(YearMonth.of(2026, 6))
        !billRecurring.isFinishedOn(YearMonth.of(2026, 5))
    }

    def "lastReferenceMonth uses the month of the end date"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 9, 2), null, true, Instant.now(), null)

        expect:
        billRecurring.lastReferenceMonth() == YearMonth.of(2026, 9)
    }

    def "validate throws DomainException when both end date and installments are informed"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 9, 2), 6, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException for a non-positive installments count"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), null, installments, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)

        where:
        installments << [0, -3]
    }

    def "validate throws DomainException when the end date precedes the start date"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 2, 10), null, true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)
    }

    def "deactivate sets active to false"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()

        when:
        billRecurring.deactivate()

        then:
        !billRecurring.isActive()
    }
}
