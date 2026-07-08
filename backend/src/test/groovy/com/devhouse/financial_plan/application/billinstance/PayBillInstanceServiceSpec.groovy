package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest
import com.devhouse.financial_plan.application.transaction.CreateTransactionService
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class PayBillInstanceServiceSpec extends Specification {

    BillRepository billRepository = Mock()
    UserRepository userRepository = Mock()
    CreateTransactionService createTransactionService = Mock()

    PayBillInstanceService service = new PayBillInstanceService(billRepository, userRepository, createTransactionService)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Bill buildInstance(Category category, BillInstanceStatus status, SubCategory subCategory = null) {
        new Bill(1L, 0, buildSpace(), null, "Energy Bill", category, subCategory, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), status, null, null, null, false, Instant.now(), null)
    }

    def "execute pays the bill using the bill's own category and subCategory"() {
        given:
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        SubCategory subCategory = new SubCategory(30L, 0, category, "Electricity", true, Instant.now(), null)
        billRepository.findById(1L) >> buildInstance(category, BillInstanceStatus.PENDING, subCategory)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        TransactionResponse transactionResponse = new TransactionResponse(99L, 0, TransactionType.EXPENSE, 1L, 2L, null,
                20L, 30L, 40L, new BigDecimal("150.00"), LocalDate.of(2026, 3, 9), "Pagamento de conta - Energy Bill", Instant.now(),
                null, null, null)
        CreateTransactionRequest capturedRequest = null
        createTransactionService.execute(_, TransactionSourceType.BILL_INSTANCE_PAYMENT, 1L) >> { CreateTransactionRequest req, srcType, srcId ->
            capturedRequest = req
            transactionResponse
        }
        billRepository.update(_) >> { Bill i -> i }
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 40L, LocalDate.of(2026, 3, 9))

        when:
        def response = service.execute(1L, request, "auth0|1")

        then:
        capturedRequest.type() == TransactionType.EXPENSE
        capturedRequest.userId() == 1L
        capturedRequest.bankAccountId() == 2L
        capturedRequest.categoryId() == 20L
        capturedRequest.subCategoryId() == 30L
        capturedRequest.amount() == new BigDecimal("150.00")
        response.status() == BillInstanceStatus.PAID
        response.paymentTransactionId() == 99L
        response.bankAccountId() == 2L
    }

    def "execute throws DomainException when the instance does not exist"() {
        given:
        billRepository.findById(99L) >> null
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(99L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the instance is already paid"() {
        given:
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        billRepository.findById(1L) >> buildInstance(category, BillInstanceStatus.PAID)
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the authenticated user cannot be resolved"() {
        given:
        Category category = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        billRepository.findById(1L) >> buildInstance(category, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|unknown") >> null
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|unknown")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the bill has no category"() {
        given:
        billRepository.findById(1L) >> buildInstance(null, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }
}
