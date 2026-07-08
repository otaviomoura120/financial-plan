package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillScheduleRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillScheduleServiceSpec extends Specification {

    BillRepository billRepository = Mock()

    UpdateBillScheduleService service = new UpdateBillScheduleService(billRepository)

    private Bill buildBill() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    def "execute updates recurring and startDate without touching name/category/defaultAmount"() {
        given:
        billRepository.findById(10L) >> buildBill()
        billRepository.update(_) >> { Bill b -> b }
        UpdateBillScheduleRequest request = new UpdateBillScheduleRequest(0, false, LocalDate.of(2026, 6, 1))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        !response.recurring()
        response.startDate() == LocalDate.of(2026, 6, 1)
        response.name() == "Energy Bill"
        response.defaultAmount() == new BigDecimal("150.00")
    }

    def "execute throws DomainException when bill does not exist"() {
        given:
        billRepository.findById(99L) >> null
        UpdateBillScheduleRequest request = new UpdateBillScheduleRequest(0, false, LocalDate.of(2026, 6, 1))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }
}
