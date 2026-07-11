package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeleteBillRecurringServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    BillRepository billRepository = Mock()
    DeleteBillRecurringService service = new DeleteBillRecurringService(billRecurringRepository, billRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring() {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private Bill buildGeneratedBill(Long id, BillRecurring billRecurring) {
        new Bill(id, 0, buildSpace(), billRecurring, "Energy Bill", null, null, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING, null, null, null,
                false, Instant.now(), null)
    }

    def "execute hard-deletes the bill recurring and detaches generated bills"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()
        billRecurringRepository.findById(10L) >> billRecurring
        Bill bill1 = buildGeneratedBill(1L, billRecurring)
        Bill bill2 = buildGeneratedBill(2L, billRecurring)
        billRepository.findByBillRecurringId(10L) >> [bill1, bill2]

        when:
        service.execute(10L)

        then:
        bill1.getBillRecurring() == null
        bill2.getBillRecurring() == null
        1 * billRepository.update(bill1)
        1 * billRepository.update(bill2)
        1 * billRecurringRepository.delete(10L)
    }

    def "execute hard-deletes the bill recurring when it has no generated bills"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRepository.findByBillRecurringId(10L) >> []

        when:
        service.execute(10L)

        then:
        0 * billRepository.update(_)
        1 * billRecurringRepository.delete(10L)
    }

    def "execute throws DomainException when bill recurring does not exist"() {
        given:
        billRecurringRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
        0 * billRecurringRepository.delete(_)
    }
}
