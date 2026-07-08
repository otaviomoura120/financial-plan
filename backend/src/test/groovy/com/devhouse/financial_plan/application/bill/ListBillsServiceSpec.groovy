package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListBillsServiceSpec extends Specification {

    BillRepository billRepository = Mock()

    ListBillsService service = new ListBillsService(billRepository)

    private Bill buildBill(Long id, String name) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(id, 0, space, name, null, new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    def "execute returns every bill of the space"() {
        given:
        billRepository.findBySpaceId(1L) >> [buildBill(10L, "Energy Bill"), buildBill(11L, "Water Bill")]

        when:
        def result = service.execute(1L)

        then:
        result.size() == 2
        result*.name() == ["Energy Bill", "Water Bill"]
    }

    def "execute returns an empty list when the space has no bills"() {
        given:
        billRepository.findBySpaceId(1L) >> []

        when:
        def result = service.execute(1L)

        then:
        result.isEmpty()
    }
}
