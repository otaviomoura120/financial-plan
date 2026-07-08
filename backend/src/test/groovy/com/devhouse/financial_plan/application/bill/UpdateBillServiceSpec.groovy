package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    CategoryRepository categoryRepository = Mock()

    UpdateBillService service = new UpdateBillService(billRepository, categoryRepository)

    private Bill buildBill() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(10L, 0, space, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    def "execute updates name, category and defaultAmount"() {
        given:
        billRepository.findById(10L) >> buildBill()
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRepository.update(_) >> { Bill b -> b }
        UpdateBillRequest request = new UpdateBillRequest(0, "Power Bill", 20L, new BigDecimal("180.00"))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        response.name() == "Power Bill"
        response.categoryId() == 20L
        response.defaultAmount() == new BigDecimal("180.00")
    }

    def "execute throws DomainException when bill does not exist"() {
        given:
        billRepository.findById(99L) >> null
        UpdateBillRequest request = new UpdateBillRequest(0, "Power Bill", null, new BigDecimal("180.00"))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        billRepository.findById(10L) >> buildBill()
        categoryRepository.findById(20L) >> null
        UpdateBillRequest request = new UpdateBillRequest(0, "Power Bill", 20L, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when version does not match"() {
        given:
        billRepository.findById(10L) >> buildBill()
        UpdateBillRequest request = new UpdateBillRequest(99, "Power Bill", null, new BigDecimal("180.00"))

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * billRepository.update(_)
    }
}
