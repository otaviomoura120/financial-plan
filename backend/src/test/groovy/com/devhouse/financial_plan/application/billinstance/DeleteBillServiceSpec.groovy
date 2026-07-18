package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeleteBillServiceSpec extends Specification {

    BillRepository billRepository = Mock()

    DeleteBillService service = new DeleteBillService(billRepository)

    private Bill buildInstance(BillInstanceStatus status) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(1L, 0, space, null, "Energy Bill", null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute hard-deletes the bill when pending"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PENDING)
        billRepository.findById(1L) >> bill

        when:
        service.execute(1L)

        then:
        1 * billRepository.delete(1L)
    }

    def "execute throws DomainException when bill does not exist"() {
        given:
        billRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * billRepository.delete(_)
    }

    def "execute throws DomainException when the bill is already paid"() {
        given:
        Bill bill = buildInstance(BillInstanceStatus.PAID)
        billRepository.findById(1L) >> bill

        when:
        service.execute(1L)

        then:
        thrown(DomainException)
        0 * billRepository.delete(_)
    }
}
