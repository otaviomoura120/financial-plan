package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeleteCreditCardTransactionServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    DeleteCreditCardTransactionService service = new DeleteCreditCardTransactionService(creditCardTransactionRepository,
            creditCardInvoicePaymentRepository)

    private CreditCardTransaction buildTransaction() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        new CreditCardTransaction(1L, 0, creditCard, user, category, null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
    }

    def "execute deletes the transaction when the invoice is still open"() {
        given:
        creditCardTransactionRepository.findById(1L) >> buildTransaction()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null

        when:
        service.execute(1L)

        then:
        1 * creditCardTransactionRepository.delete(1L)
    }

    def "execute throws DomainException when transaction does not exist"() {
        given:
        creditCardTransactionRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.delete(_)
    }

    def "execute throws DomainException when the invoice month is already paid"() {
        given:
        creditCardTransactionRepository.findById(1L) >> buildTransaction()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)

        when:
        service.execute(1L)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.delete(_)
    }
}
