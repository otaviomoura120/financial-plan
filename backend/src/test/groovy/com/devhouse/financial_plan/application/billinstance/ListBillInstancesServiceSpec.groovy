package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListBillInstancesServiceSpec extends Specification {

    EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService = Mock()
    BillRepository billRepository = Mock()

    ListBillInstancesService service = new ListBillInstancesService(ensureRecurringBillsGeneratedService, billRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private Bill buildInstance(BillRecurring billRecurring = null) {
        new Bill(1L, 0, buildSpace(), billRecurring, "Energy Bill", null, null, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null, false,
                Instant.now(), null)
    }

    def "execute ensures instances are generated up to 'to' and returns the period's bills"() {
        given:
        BillRecurring billRecurring = new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)
        billRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >> [buildInstance(billRecurring)]

        when:
        def result = service.execute(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))

        then:
        1 * ensureRecurringBillsGeneratedService.execute(1L, LocalDate.of(2026, 3, 31))
        result.size() == 1
        result[0].billRecurringId() == 10L
        result[0].name() == "Energy Bill"
    }

    def "execute returns a bill with no billRecurringId when it is standalone"() {
        given:
        billRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >> [buildInstance()]

        when:
        def result = service.execute(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))

        then:
        result[0].billRecurringId() == null
    }

    def "execute uses today as the ensure cap when 'to' is not informed"() {
        given:
        billRepository.findBySpaceAndPeriod(1L, null, null) >> []

        when:
        service.execute(1L, null, null)

        then:
        1 * ensureRecurringBillsGeneratedService.execute(1L, LocalDate.now())
    }

    def "execute returns an empty list when the space has no bills in the period"() {
        given:
        billRepository.findBySpaceAndPeriod(1L, _, _) >> []

        when:
        def result = service.execute(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

        then:
        result.isEmpty()
    }
}
