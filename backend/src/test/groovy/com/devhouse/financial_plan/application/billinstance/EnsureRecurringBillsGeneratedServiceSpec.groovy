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

    static final int HORIZON_MONTHS = 12
    static final int MAX_HORIZON_MONTHS = 60

    BillRecurringRepository billRecurringRepository = Mock()
    BillRepository billRepository = Mock()

    EnsureRecurringBillsGeneratedService service =
            new EnsureRecurringBillsGeneratedService(billRecurringRepository, billRepository, HORIZON_MONTHS, MAX_HORIZON_MONTHS)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring(LocalDate startDate, boolean active, LocalDate endDate = null, Integer installments = null) {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null, new BigDecimal("150.00"), startDate, endDate,
                installments, active, Instant.now(), null)
    }

    private Bill buildInstance(LocalDate referenceMonth) {
        new Bill(1L, 0, buildSpace(), buildBillRecurring(referenceMonth, true), "Energy Bill", null, null, referenceMonth,
                referenceMonth, new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, false, Instant.now(), null)
    }

    def "execute generates every missing month from the startDate up to the 12-month horizon"() {
        given:
        YearMonth startMonth = YearMonth.now().minusMonths(2)
        BillRecurring billRecurring = buildBillRecurring(startMonth.atDay(10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now())

        then:
        saved.size() == 15
        saved.first().referenceMonth == startMonth.atDay(1)
        saved.last().referenceMonth == YearMonth.now().plusMonths(HORIZON_MONTHS).atDay(1)
        saved.every { it.dueDate.dayOfMonth == 10 }
    }

    def "execute honours a period filter that reaches beyond the default horizon"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now().plusMonths(18))

        then:
        saved.last().referenceMonth == YearMonth.now().plusMonths(18).atDay(1)
        saved.size() == 19
    }

    def "execute caps generation at the safety ceiling when upToDate is absurdly far away"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(5), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now().plusYears(20))

        then:
        saved.last().referenceMonth == YearMonth.now().plusMonths(MAX_HORIZON_MONTHS).atDay(1)
        saved.size() == MAX_HORIZON_MONTHS + 1
    }

    def "execute only generates months after the last already-generated one"() {
        given:
        YearMonth startMonth = YearMonth.now().minusMonths(2)
        BillRecurring billRecurring = buildBillRecurring(startMonth.atDay(10), true)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> [buildInstance(startMonth.atDay(1)), buildInstance(startMonth.plusMonths(1).atDay(1))]
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now())

        then:
        saved.size() == 13
        saved.first().referenceMonth == YearMonth.now().atDay(1)
    }

    def "execute is idempotent: does not recreate a month that already exists"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), true, YearMonth.now().atEndOfMonth())
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, YearMonth.now().atDay(1)) >> buildInstance(YearMonth.now().atDay(1))

        when:
        service.execute(1L, LocalDate.now())

        then:
        0 * billRepository.save(_)
    }

    def "execute stops at the recurrence end date"() {
        given:
        YearMonth endMonth = YearMonth.now().plusMonths(3)
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), true, endMonth.atDay(20))
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now().plusYears(1))

        then:
        saved.size() == 4
        saved.last().referenceMonth == endMonth.atDay(1)
    }

    def "execute generates exactly the configured number of installments"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), true, null, 3)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.execute(1L, LocalDate.now().plusYears(1))

        then:
        saved.size() == 3
        saved.last().referenceMonth == YearMonth.now().plusMonths(2).atDay(1)
    }

    def "execute skips bill recurrings that are inactive"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), false)
        billRecurringRepository.findBySpaceId(1L) >> [billRecurring]

        when:
        service.execute(1L, LocalDate.now())

        then:
        0 * billRepository.findByBillRecurringId(_)
        0 * billRepository.save(_)
    }

    def "executeForRecurring materializes the whole horizon without consulting the space"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(28), true)
        billRepository.findByBillRecurringId(10L) >> []
        billRepository.findByBillRecurringIdAndReferenceMonth(10L, _) >> null
        List<Bill> saved = []
        billRepository.save(_) >> { Bill bill -> saved << bill; bill }

        when:
        service.executeForRecurring(billRecurring)

        then:
        0 * billRecurringRepository.findBySpaceId(_)
        saved.size() == HORIZON_MONTHS + 1
        saved.last().referenceMonth == YearMonth.now().plusMonths(HORIZON_MONTHS).atDay(1)
    }

    def "executeForRecurring ignores an inactive recurrence"() {
        given:
        BillRecurring billRecurring = buildBillRecurring(YearMonth.now().atDay(10), false)

        when:
        service.executeForRecurring(billRecurring)

        then:
        0 * billRepository.save(_)
    }
}
