package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class EnsureRecurringBillsGeneratedServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    BillRepository billRepository = Mock()

    EnsureRecurringBillsGeneratedService service = new EnsureRecurringBillsGeneratedService(billRecurringRepository, billRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring(LocalDate startDate, boolean active) {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null, new BigDecimal("150.00"), startDate, active,
                Instant.now(), null)
    }

    private Bill buildInstance(LocalDate referenceMonth) {
        new Bill(1L, 0, buildSpace(), buildBillRecurring(referenceMonth, true), "Energy Bill", null, null, referenceMonth,
                referenceMonth, new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, false, Instant.now(), null)
    }

    def "execute generates every missing month from the bill recurring's startDate up to the requested month"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(LocalDate.of(2026, 1, 10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        saved.size() == 3
        saved*.referenceMonth == [LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1)]
        saved*.dueDate == [LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10)]
    }

    def "execute only generates months after the last already-generated one"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(LocalDate.of(2026, 1, 10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> [buildInstance(LocalDate.of(2026, 1, 1)), buildInstance(LocalDate.of(2026, 2, 1))]
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        saved.size() == 1
        saved[0].referenceMonth == LocalDate.of(2026, 3, 1)
    }

    def "execute is idempotent: does not recreate a month that already exists"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(LocalDate.of(2026, 1, 10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> buildInstance(LocalDate.of(2026, 1, 1))
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null

        when:
        service.execute(1L, LocalDate.of(2026, 1, 20))

        then:
        0 * billRepository.save(_)
    }

    def "execute caps generation at current month plus one, even when upToDate is far in the future"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().minusMonths(1).atDay(5), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.now().plusYears(5))

        then:
        saved.every { !YearMonth.from(it.referenceMonth).isAfter(YearMonth.now().plusMonths(1)) }
        saved.size() == 3
    }

    def "execute skips bill recurrings that are inactive"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(LocalDate.of(2026, 1, 10), false)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        0 * billRepository.findByBillRecurringId(_)
        0 * billRepository.save(_)
    }
}
