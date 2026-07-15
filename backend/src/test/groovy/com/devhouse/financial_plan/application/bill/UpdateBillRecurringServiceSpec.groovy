package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class UpdateBillRecurringServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    BillRepository billRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    UpdateBillRecurringService service = new UpdateBillRecurringService(billRecurringRepository, billRepository,
            categoryRepository, subCategoryRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BillRecurring buildBillRecurring() {
        new BillRecurring(10L, 0, buildSpace(), "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private Bill buildGeneratedBill(Long id, LocalDate referenceMonth, BillInstanceStatus status = BillInstanceStatus.PENDING) {
        new Bill(id, 0, buildSpace(), buildBillRecurring(), "Energy Bill", null, null, referenceMonth,
                referenceMonth, new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute updates name, category and defaultAmount"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        billRepository.findByBillRecurringId(10L) >> []
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", 20L, null, new BigDecimal("180.00"))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        response.name() == "Power Bill"
        response.categoryId() == 20L
        response.defaultAmount() == new BigDecimal("180.00")
    }

    def "execute also updates already-generated pending bills from the current month onward"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        Bill pastBill = buildGeneratedBill(1L, YearMonth.now().minusMonths(2).atDay(1))
        Bill currentMonthBill = buildGeneratedBill(2L, YearMonth.now().atDay(1))
        Bill futureBill = buildGeneratedBill(3L, YearMonth.now().plusMonths(3).atDay(1))
        billRepository.findByBillRecurringId(10L) >> [pastBill, currentMonthBill, futureBill]
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", 20L, null, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        currentMonthBill.getName() == "Power Bill"
        currentMonthBill.getCategory() == category
        currentMonthBill.getAmount() == new BigDecimal("180.00")
        futureBill.getName() == "Power Bill"
        futureBill.getAmount() == new BigDecimal("180.00")
        pastBill.getName() == "Energy Bill"
        pastBill.getAmount() == new BigDecimal("150.00")
        1 * billRepository.update(currentMonthBill)
        1 * billRepository.update(futureBill)
        0 * billRepository.update(pastBill)
    }

    def "execute does not update a current/future bill that is already paid"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        Bill paidBill = buildGeneratedBill(1L, YearMonth.now().atDay(1), BillInstanceStatus.PAID)
        billRepository.findByBillRecurringId(10L) >> [paidBill]
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", 20L, null, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        paidBill.getName() == "Energy Bill"
        0 * billRepository.update(paidBill)
    }

    def "execute throws DomainException when bill recurring does not exist"() {
        given:
        billRecurringRepository.findById(99L) >> null
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", null, null, new BigDecimal("180.00"))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.update(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        categoryRepository.findById(20L) >> null
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", 20L, null, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when version does not match"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(99, "Power Bill", null, null, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * billRecurringRepository.update(_)
    }
}
