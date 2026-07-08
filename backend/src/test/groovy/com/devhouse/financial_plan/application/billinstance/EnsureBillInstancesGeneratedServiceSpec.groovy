package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class EnsureBillInstancesGeneratedServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    BillInstanceRepository billInstanceRepository = Mock()

    EnsureBillInstancesGeneratedService service = new EnsureBillInstancesGeneratedService(billRepository, billInstanceRepository)

    private Bill buildBill(LocalDate startDate, boolean recurring, boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"), startDate, recurring, active, Instant.now(), null)
    }

    private BillInstance buildInstance(LocalDate referenceMonth) {
        new BillInstance(1L, 0, buildBill(referenceMonth, true, true), referenceMonth, referenceMonth, new BigDecimal("150.00"),
                BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)
    }

    def "execute generates every missing month from the bill's startDate up to the requested month"() {
        given:
        Bill bill = buildBill(LocalDate.of(2026, 1, 10), true, true)
        billRepository.findBySpaceId(1L) >> [bill]
        billInstanceRepository.findByBillId(10L) >> []
        billInstanceRepository.findByBillIdAndReferenceMonth(10L, _) >> null
        List<BillInstance> saved = []
        billInstanceRepository.save(_) >> { BillInstance i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        saved.size() == 3
        saved*.referenceMonth == [LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1)]
        saved*.dueDate == [LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10), LocalDate.of(2026, 3, 10)]
    }

    def "execute only generates months after the last already-generated one"() {
        given:
        Bill bill = buildBill(LocalDate.of(2026, 1, 10), true, true)
        billRepository.findBySpaceId(1L) >> [bill]
        billInstanceRepository.findByBillId(10L) >> [buildInstance(LocalDate.of(2026, 1, 1)), buildInstance(LocalDate.of(2026, 2, 1))]
        billInstanceRepository.findByBillIdAndReferenceMonth(10L, _) >> null
        List<BillInstance> saved = []
        billInstanceRepository.save(_) >> { BillInstance i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        saved.size() == 1
        saved[0].referenceMonth == LocalDate.of(2026, 3, 1)
    }

    def "execute is idempotent: does not recreate a month that already exists"() {
        given:
        Bill bill = buildBill(LocalDate.of(2026, 1, 10), true, true)
        billRepository.findBySpaceId(1L) >> [bill]
        billInstanceRepository.findByBillId(10L) >> []
        billInstanceRepository.findByBillIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> buildInstance(LocalDate.of(2026, 1, 1))
        billInstanceRepository.findByBillIdAndReferenceMonth(10L, _) >> null

        when:
        service.execute(1L, LocalDate.of(2026, 1, 20))

        then:
        0 * billInstanceRepository.save(_)
    }

    def "execute caps generation at current month plus one, even when upToDate is far in the future"() {
        given:
        Bill bill = buildBill(YearMonth.now().minusMonths(1).atDay(5), true, true)
        billRepository.findBySpaceId(1L) >> [bill]
        billInstanceRepository.findByBillId(10L) >> []
        billInstanceRepository.findByBillIdAndReferenceMonth(10L, _) >> null
        List<BillInstance> saved = []
        billInstanceRepository.save(_) >> { BillInstance i -> saved << i; i }

        when:
        service.execute(1L, LocalDate.now().plusYears(5))

        then:
        saved.every { !YearMonth.from(it.referenceMonth).isAfter(YearMonth.now().plusMonths(1)) }
        saved.size() == 3
    }

    def "execute skips bills that are not recurring"() {
        given:
        Bill bill = buildBill(LocalDate.of(2026, 1, 10), false, true)
        billRepository.findBySpaceId(1L) >> [bill]

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        0 * billInstanceRepository.findByBillId(_)
        0 * billInstanceRepository.save(_)
    }

    def "execute skips bills that are inactive"() {
        given:
        Bill bill = buildBill(LocalDate.of(2026, 1, 10), true, false)
        billRepository.findBySpaceId(1L) >> [bill]

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        0 * billInstanceRepository.findByBillId(_)
        0 * billInstanceRepository.save(_)
    }
}
