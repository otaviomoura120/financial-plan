package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateCreditCardTransactionServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    UpdateCreditCardTransactionService service = new UpdateCreditCardTransactionService(creditCardTransactionRepository,
            categoryRepository, subCategoryRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private CreditCardTransaction buildExisting() {
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        new CreditCardTransaction(1L, 0, buildCreditCard(), buildUser(), category, null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
    }

    def "execute updates category, amount, purchaseDate and description"() {
        given:
        CreditCardTransaction existing = buildExisting()
        creditCardTransactionRepository.findById(1L) >> existing
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        categoryRepository.findById(21L) >> new Category(21L, 0, null, "Travel", true, Instant.now(), null)
        creditCardTransactionRepository.update(_) >> { CreditCardTransaction t -> t }
        UpdateCreditCardTransactionRequest request = new UpdateCreditCardTransactionRequest(0, 21L, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "new desc")

        when:
        CreditCardTransactionResponse response = service.execute(1L, request)

        then:
        response.categoryId() == 21L
        response.amount() == new BigDecimal("150.00")
        response.purchaseDate() == LocalDate.of(2026, 3, 10)
        response.description() == "new desc"
        response.referenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "execute throws DomainException when transaction does not exist"() {
        given:
        creditCardTransactionRepository.findById(99L) >> null
        UpdateCreditCardTransactionRequest request = new UpdateCreditCardTransactionRequest(0, 21L, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "new desc")

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws DomainException when the invoice month is already paid"() {
        given:
        CreditCardTransaction existing = buildExisting()
        creditCardTransactionRepository.findById(1L) >> existing
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)
        UpdateCreditCardTransactionRequest request = new UpdateCreditCardTransactionRequest(0, 21L, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "new desc")

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws DomainException when new category does not exist"() {
        given:
        CreditCardTransaction existing = buildExisting()
        creditCardTransactionRepository.findById(1L) >> existing
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        categoryRepository.findById(99L) >> null
        UpdateCreditCardTransactionRequest request = new UpdateCreditCardTransactionRequest(0, 99L, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "new desc")

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        CreditCardTransaction existing = buildExisting()
        creditCardTransactionRepository.findById(1L) >> existing
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        UpdateCreditCardTransactionRequest request = new UpdateCreditCardTransactionRequest(99, 21L, null,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "new desc")

        when:
        service.execute(1L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * creditCardTransactionRepository.update(_)
    }
}
