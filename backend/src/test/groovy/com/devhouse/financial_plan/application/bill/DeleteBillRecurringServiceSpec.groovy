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
import java.time.YearMonth

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

    private Bill buildGeneratedBill(Long id, BillRecurring billRecurring, LocalDate referenceMonth, BillInstanceStatus status = BillInstanceStatus.PENDING) {
        new Bill(id, 0, buildSpace(), billRecurring, "Energy Bill", null, null, referenceMonth,
                referenceMonth, new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute deletes pending bills from the current month onward and detaches (keeps) past bills"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()
        billRecurringRepository.findById(10L) >> billRecurring
        Bill pastBill = buildGeneratedBill(1L, billRecurring, YearMonth.now().minusMonths(2).atDay(1))
        Bill currentMonthBill = buildGeneratedBill(2L, billRecurring, YearMonth.now().atDay(1))
        Bill futureBill = buildGeneratedBill(3L, billRecurring, YearMonth.now().plusMonths(3).atDay(1))
        billRepository.findByBillRecurringId(10L) >> [pastBill, currentMonthBill, futureBill]

        when:
        service.execute(10L)

        then:
        pastBill.getBillRecurring() == null
        1 * billRepository.update(pastBill)
        0 * billRepository.update(currentMonthBill)
        0 * billRepository.update(futureBill)
        1 * billRepository.delete(2L)
        1 * billRepository.delete(3L)
        0 * billRepository.delete(1L)
        1 * billRecurringRepository.delete(10L)
    }

    def "execute detaches (does not delete) a current/future bill that is already paid"() {
        given:
        BillRecurring billRecurring = buildBillRecurring()
        billRecurringRepository.findById(10L) >> billRecurring
        Bill paidBill = buildGeneratedBill(1L, billRecurring, YearMonth.now().atDay(1), BillInstanceStatus.PAID)
        billRepository.findByBillRecurringId(10L) >> [paidBill]

        when:
        service.execute(10L)

        then:
        paidBill.getBillRecurring() == null
        1 * billRepository.update(paidBill)
        0 * billRepository.delete(1L)
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
        0 * billRepository.delete(_)
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
        0 * billRepository.delete(_)
        0 * billRecurringRepository.delete(_)
    }
}
