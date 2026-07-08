package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest
import com.devhouse.financial_plan.application.transaction.CreateTransactionService
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class PayBillInstanceServiceSpec extends Specification {

    BillInstanceRepository billInstanceRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    UserRepository userRepository = Mock()
    CreateTransactionService createTransactionService = Mock()

    PayBillInstanceService service = new PayBillInstanceService(billInstanceRepository, categoryRepository,
            userRepository, createTransactionService)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Bill buildBill(Category category) {
        new Bill(10L, 0, buildSpace(), "Energy Bill", category, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    private BillInstance buildInstance(Bill bill, BillInstanceStatus status) {
        new BillInstance(1L, 0, bill, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"),
                status, null, null, null, Instant.now(), null)
    }

    def "execute pays the instance using the category from the request"() {
        given:
        Category defaultCategory = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        Category requestCategory = new Category(21L, 0, buildSpace(), "Housing", true, Instant.now(), null)
        Bill bill = buildBill(defaultCategory)
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        categoryRepository.findById(21L) >> requestCategory
        TransactionResponse transactionResponse = new TransactionResponse(99L, 0, TransactionType.EXPENSE, 1L, 2L, null,
                21L, null, 40L, new BigDecimal("150.00"), LocalDate.of(2026, 3, 9), "Pagamento de conta - Energy Bill", Instant.now())
        CreateTransactionRequest capturedRequest = null
        createTransactionService.execute(_, TransactionSourceType.BILL_INSTANCE_PAYMENT, 10L) >> { CreateTransactionRequest req, srcType, srcId ->
            capturedRequest = req
            transactionResponse
        }
        billInstanceRepository.update(_) >> { BillInstance i -> i }
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 21L, 40L, LocalDate.of(2026, 3, 9))

        when:
        def response = service.execute(1L, request, "auth0|1")

        then:
        capturedRequest.type() == TransactionType.EXPENSE
        capturedRequest.userId() == 1L
        capturedRequest.bankAccountId() == 2L
        capturedRequest.categoryId() == 21L
        capturedRequest.amount() == new BigDecimal("150.00")
        response.status() == BillInstanceStatus.PAID
        response.paymentTransactionId() == 99L
        response.bankAccountId() == 2L
    }

    def "execute falls back to the bill's default category when none is informed"() {
        given:
        Category defaultCategory = new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null)
        Bill bill = buildBill(defaultCategory)
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        CreateTransactionRequest capturedRequest = null
        createTransactionService.execute(_, TransactionSourceType.BILL_INSTANCE_PAYMENT, 10L) >> { CreateTransactionRequest req, srcType, srcId ->
            capturedRequest = req
            new TransactionResponse(99L, 0, TransactionType.EXPENSE, 1L, 2L, null, 20L, null, 40L,
                    new BigDecimal("150.00"), LocalDate.of(2026, 3, 9), "desc", Instant.now())
        }
        billInstanceRepository.update(_) >> { BillInstance i -> i }
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, null, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        capturedRequest.categoryId() == 20L
        0 * categoryRepository.findById(_)
    }

    def "execute throws DomainException when the instance does not exist"() {
        given:
        billInstanceRepository.findById(99L) >> null
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, null, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(99L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the instance is already paid"() {
        given:
        Bill bill = buildBill(new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null))
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PAID)
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, null, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the authenticated user cannot be resolved"() {
        given:
        Bill bill = buildBill(new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null))
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|unknown") >> null
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, null, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|unknown")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the requested category does not exist"() {
        given:
        Bill bill = buildBill(new Category(20L, 0, buildSpace(), "Utilities", true, Instant.now(), null))
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        categoryRepository.findById(99L) >> null
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, 99L, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when no category is informed and the bill has no default category"() {
        given:
        Bill bill = buildBill(null)
        billInstanceRepository.findById(1L) >> buildInstance(bill, BillInstanceStatus.PENDING)
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        PayBillInstanceRequest request = new PayBillInstanceRequest(2L, null, 40L, LocalDate.of(2026, 3, 9))

        when:
        service.execute(1L, request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }
}
