package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeactivateBillRecurringServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()

    DeactivateBillRecurringService service = new DeactivateBillRecurringService(billRecurringRepository)

    private BillRecurring buildBillRecurring() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BillRecurring(10L, 0, space, "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "execute deactivates the bill recurring"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()
        billRecurringRepository.findById(10L) >> billRecurring

        when:
        service.execute(10L)

        then:
        !billRecurring.isActive()
        1 * billRecurringRepository.update(billRecurring)
    }

    def "execute throws DomainException when bill recurring does not exist"() {
        given:
        billRecurringRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.update(_)
    }
}
