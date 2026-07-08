package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.UpdateBillRecurringRequest
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateBillRecurringServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    UpdateBillRecurringService service = new UpdateBillRecurringService(billRecurringRepository, categoryRepository, subCategoryRepository)

    private BillRecurring buildBillRecurring() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BillRecurring(10L, 0, space, "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "execute updates name, category and defaultAmount"() {
        given:
        billRecurringRepository.findById(10L) >> buildBillRecurring()
        Category category = new Category(20L, 0, null, "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRecurringRepository.update(_) >> { BillRecurring b -> b }
        UpdateBillRecurringRequest request = new UpdateBillRecurringRequest(0, "Power Bill", 20L, null, new BigDecimal("180.00"))

        when:
        BillResponse response = service.execute(10L, request)

        then:
        response.name() == "Power Bill"
        response.categoryId() == 20L
        response.defaultAmount() == new BigDecimal("180.00")
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
