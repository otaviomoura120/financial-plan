package com.devhouse.financial_plan.application.creditcardinvoice

import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoiceResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListCreditCardInvoicesServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    ListCreditCardInvoicesService service = new ListCreditCardInvoicesService(creditCardRepository,
            creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard(Long id) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(id, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private CreditCardTransaction buildTransaction(CreditCard creditCard, BigDecimal amount, LocalDate referenceMonth) {
        new CreditCardTransaction(1L, 0, creditCard, buildUser(), new Category(20L, 0, null, "Food", true, Instant.now(), null),
                null, amount, LocalDate.of(2026, 1, 5), "desc", referenceMonth, "group-1", 1, 1, false, null,
                Instant.now(), null)
    }

    def "execute marks each group as paid or open based on CreditCardInvoicePayment existence"() {
        given:
        CreditCard creditCard = buildCreditCard(10L)
        creditCardRepository.findBySpaceId(1L) >> [creditCard]
        creditCardTransactionRepository.findByCreditCardId(10L) >> [
                buildTransaction(creditCard, new BigDecimal("100.00"), LocalDate.of(2026, 3, 1)),
                buildTransaction(creditCard, new BigDecimal("200.00"), LocalDate.of(2026, 4, 1))
        ]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        CreditCardInvoicePayment payment = new CreditCardInvoicePayment(5L, 0, creditCard, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 17), new BigDecimal("200.00"), LocalDate.of(2026, 4, 10), 99L, 2L, Instant.now(), null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 4, 1)) >> payment

        when:
        List<CreditCardInvoiceResponse> responses = service.execute(1L, null, null, null)

        then:
        responses.size() == 2
        CreditCardInvoiceResponse open = responses.find { it.referenceMonth() == LocalDate.of(2026, 3, 1) }
        CreditCardInvoiceResponse paid = responses.find { it.referenceMonth() == LocalDate.of(2026, 4, 1) }
        !open.paid()
        open.totalAmount() == new BigDecimal("100.00")
        open.dueDate() == LocalDate.of(2026, 3, 17)
        paid.paid()
        paid.totalAmount() == new BigDecimal("200.00")
        paid.paidAmount() == new BigDecimal("200.00")
        paid.paymentTransactionId() == 99L
    }

    def "execute filters invoices whose due date falls outside the requested period"() {
        given:
        CreditCard creditCard = buildCreditCard(10L)
        creditCardRepository.findBySpaceId(1L) >> [creditCard]
        creditCardTransactionRepository.findByCreditCardId(10L) >> [
                buildTransaction(creditCard, new BigDecimal("100.00"), LocalDate.of(2026, 3, 1)),
                buildTransaction(creditCard, new BigDecimal("200.00"), LocalDate.of(2026, 6, 1))
        ]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null

        when:
        List<CreditCardInvoiceResponse> responses = service.execute(1L, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))

        then:
        responses.size() == 1
        responses[0].referenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "execute filters by creditCardId when provided"() {
        given:
        CreditCard cardA = buildCreditCard(10L)
        CreditCard cardB = buildCreditCard(11L)
        creditCardRepository.findBySpaceId(1L) >> [cardA, cardB]
        creditCardTransactionRepository.findByCreditCardId(10L) >> [buildTransaction(cardA, new BigDecimal("100.00"), LocalDate.of(2026, 3, 1))]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null

        when:
        List<CreditCardInvoiceResponse> responses = service.execute(1L, 10L, null, null)

        then:
        responses.size() == 1
        responses[0].creditCardId() == 10L
        0 * creditCardTransactionRepository.findByCreditCardId(11L)
    }

    def "execute returns an empty list when the space has no credit cards"() {
        given:
        creditCardRepository.findBySpaceId(99L) >> []

        when:
        List<CreditCardInvoiceResponse> responses = service.execute(99L, null, null, null)

        then:
        responses.isEmpty()
    }
}
