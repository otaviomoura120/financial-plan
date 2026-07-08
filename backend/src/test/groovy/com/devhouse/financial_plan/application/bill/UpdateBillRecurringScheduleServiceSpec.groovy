package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringScheduleRequest
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillRecurringScheduleServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()

    UpdateBillRecurringScheduleService service = new UpdateBillRecurringScheduleService(billRecurringRepository)

    private BillRecurring buildBillRecurring() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BillRecurring(10L, 0, space, "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "execute updates startDate without touching name/category/defaultAmount"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 6, 1))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        response.startDate() == LocalDate.of(2026, 6, 1)
        response.name() == "Energy Bill"
        response.defaultAmount() == new BigDecimal("150.00")
    }

    def "execute throws DomainException when bill recurring does not exist"() {
        given:
        billRecurringRepository.findById(99L) >> null
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 6, 1))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.update(_)
    }
}
