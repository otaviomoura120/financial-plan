package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListBillInstancesServiceSpec extends Specification {

    EnsureBillInstancesGeneratedService ensureBillInstancesGeneratedService = Mock()
    BillInstanceRepository billInstanceRepository = Mock()

    ListBillInstancesService service = new ListBillInstancesService(ensureBillInstancesGeneratedService, billInstanceRepository)

    private BillInstance buildInstance() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Bill bill = new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
        new BillInstance(1L, 0, bill, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"),
                BillInstanceStatus.PENDING, null, null, null, Instant.now(), null)
    }

    def "execute ensures instances are generated up to 'to' and returns the period's instances"() {
        given:
        billInstanceRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >> [buildInstance()]

        when:
        def result = service.execute(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))

        then:
        1 * ensureBillInstancesGeneratedService.execute(1L, LocalDate.of(2026, 3, 31))
        result.size() == 1
        result[0].billId() == 10L
        result[0].billName() == "Energy Bill"
    }

    def "execute uses today as the ensure cap when 'to' is not informed"() {
        given:
        billInstanceRepository.findBySpaceAndPeriod(1L, null, null) >> []

        when:
        service.execute(1L, null, null)

        then:
        1 * ensureBillInstancesGeneratedService.execute(1L, LocalDate.now())
    }

    def "execute returns an empty list when the space has no instances in the period"() {
        given:
        billInstanceRepository.findBySpaceAndPeriod(1L, _, _) >> []

        when:
        def result = service.execute(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

        then:
        result.isEmpty()
    }
}
