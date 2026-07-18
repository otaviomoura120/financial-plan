package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.billinstance.dto.UpdateBillInstanceRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    UpdateBillService service = new UpdateBillService(billRepository, categoryRepository, subCategoryRepository)

    private Bill buildInstance(BillInstanceStatus status) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Bill(1L, 0, space, null, "Energy Bill", null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute updates name, category, amount and dueDate when pending"() {
        given:
        billRepository.findById(1L) >> buildInstance(BillInstanceStatus.PENDING)
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRepository.update(_) >> { Bill b -> b }
        UpdateBillInstanceRequest request = new UpdateBillInstanceRequest(0, "Power Bill", 20L, null,
                new BigDecimal("180.00"), LocalDate.of(2026, 4, 5))

        when:
        def response = service.execute(1L, request)

        then:
        response.name() == "Power Bill"
        response.categoryId() == 20L
        response.amount() == new BigDecimal("180.00")
        response.dueDate() == LocalDate.of(2026, 4, 5)
    }

    def "execute throws DomainException when bill does not exist"() {
        given:
        billRepository.findById(99L) >> null
        UpdateBillInstanceRequest request = new UpdateBillInstanceRequest(0, "Power Bill", null, null,
                new BigDecimal("180.00"), LocalDate.of(2026, 4, 5))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }

    def "execute throws DomainException when already paid"() {
        given:
        billRepository.findById(1L) >> buildInstance(BillInstanceStatus.PAID)
        UpdateBillInstanceRequest request = new UpdateBillInstanceRequest(0, "Power Bill", null, null,
                new BigDecimal("180.00"), LocalDate.of(2026, 4, 5))

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * billRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when version does not match"() {
        given:
        billRepository.findById(1L) >> buildInstance(BillInstanceStatus.PENDING)
        UpdateBillInstanceRequest request = new UpdateBillInstanceRequest(99, "Power Bill", null, null,
                new BigDecimal("180.00"), LocalDate.of(2026, 4, 5))

        when:
        service.execute(1L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * billRepository.update(_)
    }
}
