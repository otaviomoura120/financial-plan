package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

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
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
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
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when name is blank"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), name, buildCategory(), buildSubCategory(),
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

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
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)

        when:
        billRecurring.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when defaultAmount is null or not positive"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", buildCategory(), buildSubCategory(),
                defaultAmount, LocalDate.of(2026, 3, 10), true, Instant.now(), null)

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
                new BigDecimal("150.00"), null, true, Instant.now(), null)

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

    def "updateSchedule replaces startDate without touching the basic fields"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()

        when:
        billRecurring.updateSchedule(LocalDate.of(2026, 6, 1))

        then:
        billRecurring.getStartDate() == LocalDate.of(2026, 6, 1)
        billRecurring.getName() == "Energy Bill"
        billRecurring.getDefaultAmount() == new BigDecimal("150.00")
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
