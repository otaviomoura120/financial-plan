package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest
import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateBillServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    CreateBillService service = new CreateBillService(billRecurringRepository, spaceRepository, categoryRepository, subCategoryRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    def "execute creates a bill recurring"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        billRecurringRepository.save(_) >> { BillRecurring b -> new BillRecurring(10L, 0, b.space, b.name, b.category,
                b.subCategory, b.defaultAmount, b.startDate, b.active, b.createdDate, b.updatedDate) }
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10))

        when:
        BillResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.active()
    }

    def "execute resolves the optional category and subCategory when informed"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        SubCategory subCategory = new SubCategory(30L, 0, category, "Electricity", true, Instant.now(), null)
        subCategoryRepository.findById(30L) >> subCategory
        billRecurringRepository.save(_) >> { BillRecurring b -> new BillRecurring(10L, 0, b.space, b.name, b.category,
                b.subCategory, b.defaultAmount, b.startDate, b.active, b.createdDate, b.updatedDate) }
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", 20L, 30L, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10))

        when:
        BillResponse response = service.execute(request)

        then:
        response.categoryId() == 20L
        response.subCategoryId() == 30L
    }

    def "execute throws DomainException when space does not exist"() {
        given:
        spaceRepository.findById(1L) >> null
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", null, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        categoryRepository.findById(20L) >> null
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", 20L, null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.save(_)
    }

    def "execute throws DomainException when subCategory does not exist"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        subCategoryRepository.findById(30L) >> null
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", null, 30L, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRecurringRepository.save(_)
    }
}
