package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse
import com.devhouse.financial_plan.application.billinstance.dto.CreateBillInstanceRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateBillInstanceServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    CreateBillInstanceService service = new CreateBillInstanceService(billRepository, spaceRepository, categoryRepository, subCategoryRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    def "execute creates a standalone bill with no billRecurring, with amount and referenceMonth derived from dueDate"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        Bill saved = null
        billRepository.save(_) >> { Bill b -> saved = new Bill(10L, 0, b.space, b.billRecurring, b.name, b.category,
                b.subCategory, b.referenceMonth, b.dueDate, b.amount, b.status, b.paidDate, b.paymentTransactionId,
                b.bankAccountId, b.deleted, b.createdDate, b.updatedDate); saved }
        CreateBillInstanceRequest request = new CreateBillInstanceRequest(1L, "One-off repair", null, null,
                new BigDecimal("300.00"), LocalDate.of(2026, 3, 10))

        when:
        BillInstanceResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.billRecurringId() == null
        response.name() == "One-off repair"
        response.referenceMonth() == LocalDate.of(2026, 3, 1)
        response.dueDate() == LocalDate.of(2026, 3, 10)
        response.amount() == new BigDecimal("300.00")
        response.status() == BillInstanceStatus.PENDING
    }

    def "execute resolves the optional category and subCategory when informed"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        SubCategory subCategory = new SubCategory(30L, 0, category, "Electricity", true, Instant.now(), null)
        subCategoryRepository.findById(30L) >> subCategory
        billRepository.save(_) >> { Bill b -> new Bill(10L, 0, b.space, b.billRecurring, b.name, b.category, b.subCategory,
                b.referenceMonth, b.dueDate, b.amount, b.status, b.paidDate, b.paymentTransactionId, b.bankAccountId,
                b.deleted, b.createdDate, b.updatedDate) }
        CreateBillInstanceRequest request = new CreateBillInstanceRequest(1L, "Energy Bill", 20L, 30L,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10))

        when:
        BillInstanceResponse response = service.execute(request)

        then:
        response.categoryId() == 20L
        response.subCategoryId() == 30L
    }

    def "execute throws DomainException when space does not exist"() {
        given:
        spaceRepository.findById(1L) >> null
        CreateBillInstanceRequest request = new CreateBillInstanceRequest(1L, "One-off repair", null, null,
                new BigDecimal("300.00"), LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRepository.save(_)
    }
}
