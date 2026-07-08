package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceAmountRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillInstanceAmountServiceSpec extends Specification {

    BillInstanceRepository billInstanceRepository = Mock()

    UpdateBillInstanceAmountService service = new UpdateBillInstanceAmountService(billInstanceRepository)

    private BillInstance buildInstance(BillInstanceStatus status) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Bill bill = new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
        new BillInstance(1L, 0, bill, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"),
                status, null, null, null, Instant.now(), null)
    }

    def "execute updates the amount when the instance is pending"() {
        given:
        billInstanceRepository.findById(1L) >> buildInstance(BillInstanceStatus.PENDING)
        billInstanceRepository.update(_) >> { BillInstance i -> i }
        UpdateBillInstanceAmountRequest request = new UpdateBillInstanceAmountRequest(0, new BigDecimal("200.00"))

        when:
        def response = service.execute(1L, request)

        then:
        response.amount() == new BigDecimal("200.00")
    }

    def "execute throws DomainException when the instance does not exist"() {
        given:
        billInstanceRepository.findById(99L) >> null
        UpdateBillInstanceAmountRequest request = new UpdateBillInstanceAmountRequest(0, new BigDecimal("200.00"))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billInstanceRepository.update(_)
    }

    def "execute throws DomainException when the instance is already paid"() {
        given:
        billInstanceRepository.findById(1L) >> buildInstance(BillInstanceStatus.PAID)
        UpdateBillInstanceAmountRequest request = new UpdateBillInstanceAmountRequest(0, new BigDecimal("200.00"))

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * billInstanceRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when version does not match"() {
        given:
        billInstanceRepository.findById(1L) >> buildInstance(BillInstanceStatus.PENDING)
        UpdateBillInstanceAmountRequest request = new UpdateBillInstanceAmountRequest(99, new BigDecimal("200.00"))

        when:
        service.execute(1L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * billInstanceRepository.update(_)
    }
}
