package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListBillsServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()

    ListBillsService service = new ListBillsService(billRecurringRepository)

    private BillRecurring buildBillRecurring(Long id, String name) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BillRecurring(id, 0, space, name, null, null, new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true,
                Instant.now(), null)
    }

    def "execute returns every bill recurring of the space"() {
        given:
        billRecurringRepository.findActiveBySpaceId(1L) >> [buildBillRecurring(10L, "Energy Bill"), buildBillRecurring(11L, "Water Bill")]

        when:
        def result = service.execute(1L)

        then:
        result.size() == 2
        result*.name() == ["Energy Bill", "Water Bill"]
    }

    def "execute returns an empty list when the space has no bill recurrings"() {
        given:
        billRecurringRepository.findActiveBySpaceId(1L) >> []

        when:
        def result = service.execute(1L)

        then:
        result.isEmpty()
    }
}
