package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeactivateBillServiceSpec extends Specification {

    BillRepository billRepository = Mock()

    DeactivateBillService service = new DeactivateBillService(billRepository)

    private Bill buildBill() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    def "execute deactivates the bill"() {
        given:
        Bill bill = buildBill()
        billRepository.findById(10L) >> bill

        when:
        service.execute(10L)

        then:
        !bill.isActive()
        1 * billRepository.update(bill)
    }

    def "execute throws DomainException when bill does not exist"() {
        given:
        billRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }
}
