package com.devhouse.financial_plan.application.bill

import com.devhouse.financial_plan.application.bill.dto.BillResponse
import com.devhouse.financial_plan.application.bill.dto.CreateBillRequest
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateBillServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    BillInstanceRepository billInstanceRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CategoryRepository categoryRepository = Mock()

    CreateBillService service = new CreateBillService(billRepository, billInstanceRepository, spaceRepository, categoryRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    def "execute creates a recurring bill without generating any instance"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        Bill saved = null
        billRepository.save(_) >> { Bill b -> saved = new Bill(10L, 0, b.space, b.name, b.category, b.defaultAmount,
                b.startDate, b.recurring, b.active, b.createdDate, b.updatedDate); saved }
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true)

        when:
        BillResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.recurring()
        0 * billInstanceRepository.save(_)
    }

    def "execute creates a non-recurring bill and auto-generates a single pending instance"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        billRepository.save(_) >> { Bill b -> new Bill(10L, 0, b.space, b.name, b.category, b.defaultAmount,
                b.startDate, b.recurring, b.active, b.createdDate, b.updatedDate) }
        List<BillInstance> savedInstances = []
        billInstanceRepository.save(_) >> { BillInstance i -> savedInstances << i; i }
        CreateBillRequest request = new CreateBillRequest(1L, "One-off repair", null, new BigDecimal("300.00"),
                LocalDate.of(2026, 3, 10), false)

        when:
        service.execute(request)

        then:
        savedInstances.size() == 1
        savedInstances[0].referenceMonth == LocalDate.of(2026, 3, 1)
        savedInstances[0].dueDate == LocalDate.of(2026, 3, 10)
        savedInstances[0].amount == new BigDecimal("300.00")
        savedInstances[0].isPending()
    }

    def "execute resolves the optional category when informed"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        categoryRepository.findById(20L) >> category
        billRepository.save(_) >> { Bill b -> new Bill(10L, 0, b.space, b.name, b.category, b.defaultAmount,
                b.startDate, b.recurring, b.active, b.createdDate, b.updatedDate) }
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", 20L, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true)

        when:
        BillResponse response = service.execute(request)

        then:
        response.categoryId() == 20L
    }

    def "execute throws DomainException when space does not exist"() {
        given:
        spaceRepository.findById(1L) >> null
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        categoryRepository.findById(20L) >> null
        CreateBillRequest request = new CreateBillRequest(1L, "Energy Bill", 20L, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * billRepository.save(_)
    }
}
