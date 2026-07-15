package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringScheduleRequest
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

class UpdateBillRecurringScheduleServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    BillRepository billRepository = Mock()

    UpdateBillRecurringScheduleService service = new UpdateBillRecurringScheduleService(billRecurringRepository, billRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring() {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private Bill buildGeneratedBill(Long id, LocalDate referenceMonth, LocalDate dueDate, BillInstanceStatus status = BillInstanceStatus.PENDING) {
        new Bill(id, 0, buildSpace(), buildBillRecurring(), "Energy Bill", null, null, referenceMonth,
                dueDate, new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute updates startDate without touching name/category/defaultAmount"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        billRepository.findByBillRecurringId(10L) >> []
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 6, 1))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        response.startDate() == LocalDate.of(2026, 6, 1)
        response.name() == "Energy Bill"
        response.defaultAmount() == new BigDecimal("150.00")
    }

    def "execute recalculates dueDate for pending bills from the current month onward"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        YearMonth pastMonth = YearMonth.now().minusMonths(2)
        YearMonth currentMonth = YearMonth.now()
        YearMonth futureMonth = YearMonth.now().plusMonths(3)
        Bill pastBill = buildGeneratedBill(1L, pastMonth.atDay(1), pastMonth.atDay(10))
        Bill currentMonthBill = buildGeneratedBill(2L, currentMonth.atDay(1), currentMonth.atDay(10))
        Bill futureBill = buildGeneratedBill(3L, futureMonth.atDay(1), futureMonth.atDay(10))
        billRepository.findByBillRecurringId(10L) >> [pastBill, currentMonthBill, futureBill]
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 1, 25))

        when:
        service.execute(10L, request)

        then:
        currentMonthBill.getDueDate() == currentMonth.atDay(25)
        futureBill.getDueDate() == futureMonth.atDay(25)
        pastBill.getDueDate() == pastMonth.atDay(10)
        1 * billRepository.update(currentMonthBill)
        1 * billRepository.update(futureBill)
        0 * billRepository.update(pastBill)
    }

    def "execute clamps the recalculated dueDate to the target month's length"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        YearMonth currentMonth = YearMonth.now()
        Bill currentMonthBill = buildGeneratedBill(1L, currentMonth.atDay(1), currentMonth.atDay(10))
        billRepository.findByBillRecurringId(10L) >> [currentMonthBill]
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 1, 31))

        when:
        service.execute(10L, request)

        then:
        currentMonthBill.getDueDate() == currentMonth.atDay(Math.min(31, currentMonth.lengthOfMonth()))
    }

    def "execute does not recalculate dueDate for an already-paid bill"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        YearMonth currentMonth = YearMonth.now()
        Bill paidBill = buildGeneratedBill(1L, currentMonth.atDay(1), currentMonth.atDay(10), BillInstanceStatus.PAID)
        billRepository.findByBillRecurringId(10L) >> [paidBill]
        UpdateBillRecurringScheduleRequest request = new UpdateBillRecurringScheduleRequest(0, LocalDate.of(2026, 1, 25))

        when:
        service.execute(10L, request)

        then:
        paidBill.getDueDate() == currentMonth.atDay(10)
        0 * billRepository.update(paidBill)
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
