package com.devhouse.financial_plan.application.creditcardinvoice

import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoicePaymentResponse
import com.devhouse.financial_plan.application.creditcardinvoice.dto.PayCreditCardInvoiceRequest
import com.devhouse.financial_plan.application.transaction.CreateTransactionService
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class PayCreditCardInvoiceServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    UserRepository userRepository = Mock()
    CreateTransactionService createTransactionService = Mock()

    PayCreditCardInvoiceService service = new PayCreditCardInvoiceService(creditCardRepository,
            creditCardTransactionRepository, creditCardInvoicePaymentRepository, userRepository, createTransactionService)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private CreditCardTransaction buildTransaction(BigDecimal amount) {
        new CreditCardTransaction(1L, 0, buildCreditCard(), buildUser(), new Category(20L, 0, null, "Food", true, Instant.now(), null),
                null, amount, LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1), "group-1", 1, 1, false, null,
                Instant.now(), null)
    }

    def "execute pays the invoice: creates the EXPENSE transaction linked to the credit card and persists the payment"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        creditCardTransactionRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >>
                [buildTransaction(new BigDecimal("100.00")), buildTransaction(new BigDecimal("50.00"))]
        TransactionResponse transactionResponse = new TransactionResponse(99L, 0, TransactionType.EXPENSE, 1L, 2L, null,
                30L, null, 40L, new BigDecimal("150.00"), LocalDate.of(2026, 4, 5), "Pagamento de fatura - Nubank", Instant.now(),
                null, null, null)
        CreateTransactionRequest capturedRequest = null
        createTransactionService.execute(_, TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 10L) >> { CreateTransactionRequest req, srcType, srcId ->
            capturedRequest = req
            transactionResponse
        }
        creditCardInvoicePaymentRepository.save(_) >> { CreditCardInvoicePayment p -> p }
        PayCreditCardInvoiceRequest request = new PayCreditCardInvoiceRequest(2L, 30L, 40L, LocalDate.of(2026, 4, 5))

        when:
        CreditCardInvoicePaymentResponse response = service.execute(10L, LocalDate.of(2026, 3, 1), request, "auth0|1")

        then:
        capturedRequest.type() == TransactionType.EXPENSE
        capturedRequest.userId() == 1L
        capturedRequest.bankAccountId() == 2L
        capturedRequest.amount() == new BigDecimal("150.00")
        response.creditCardId() == 10L
        response.referenceMonth() == LocalDate.of(2026, 3, 1)
        response.dueDate() == LocalDate.of(2026, 3, 17)
        response.paidAmount() == new BigDecimal("150.00")
        response.paymentTransactionId() == 99L
        response.bankAccountId() == 2L
    }

    def "execute throws DomainException when the invoice is already paid"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)
        PayCreditCardInvoiceRequest request = new PayCreditCardInvoiceRequest(2L, 30L, 40L, LocalDate.of(2026, 4, 5))

        when:
        service.execute(10L, LocalDate.of(2026, 3, 1), request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
        0 * creditCardInvoicePaymentRepository.save(_)
    }

    def "execute throws DomainException when the invoice has no transactions to pay"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> buildUser()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        creditCardTransactionRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> []
        PayCreditCardInvoiceRequest request = new PayCreditCardInvoiceRequest(2L, 30L, 40L, LocalDate.of(2026, 4, 5))

        when:
        service.execute(10L, LocalDate.of(2026, 3, 1), request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
        0 * creditCardInvoicePaymentRepository.save(_)
    }

    def "execute throws DomainException when credit card does not exist"() {
        given:
        creditCardRepository.findById(99L) >> null
        PayCreditCardInvoiceRequest request = new PayCreditCardInvoiceRequest(2L, 30L, 40L, LocalDate.of(2026, 4, 5))

        when:
        service.execute(99L, LocalDate.of(2026, 3, 1), request, "auth0|1")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }

    def "execute throws DomainException when the authenticated user cannot be resolved"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|unknown") >> null
        PayCreditCardInvoiceRequest request = new PayCreditCardInvoiceRequest(2L, 30L, 40L, LocalDate.of(2026, 4, 5))

        when:
        service.execute(10L, LocalDate.of(2026, 3, 1), request, "auth0|unknown")

        then:
        thrown(DomainException)
        0 * createTransactionService.execute(*_)
    }
}
