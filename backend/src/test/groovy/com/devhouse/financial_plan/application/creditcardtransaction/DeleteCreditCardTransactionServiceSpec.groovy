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

    private CreditCardTransaction buildTransaction(Long id = 1L, Integer installmentNumber = 1, Integer totalInstallments = 1,
                                                     LocalDate referenceMonth = LocalDate.of(2026, 3, 1), String groupId = "group-1") {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        new CreditCardTransaction(id, 0, creditCard, null, user, category, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", referenceMonth,
                groupId, installmentNumber, totalInstallments, false, null, Instant.now(), null)
    }

    def "execute deletes the transaction when the invoice is still open"() {
        given:
        creditCardTransactionRepository.findById(1L) >> buildTransaction()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null

        when:
        service.execute(1L, false)

        then:
        1 * creditCardTransactionRepository.delete(1L)
    }

    def "execute throws DomainException when transaction does not exist"() {
        given:
        creditCardTransactionRepository.findById(99L) >> null

        when:
        service.execute(99L, false)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.delete(_)
    }

    def "execute throws DomainException when the invoice month is already paid"() {
        given:
        creditCardTransactionRepository.findById(1L) >> buildTransaction()
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)

        when:
        service.execute(1L, false)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.delete(_)
    }

    def "execute with includeFuture=true on a single cash purchase behaves like a single delete"() {
        given:
        creditCardTransactionRepository.findById(1L) >> buildTransaction(1L, 1, 1, LocalDate.of(2026, 3, 1))
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null

        when:
        service.execute(1L, true)

        then:
        0 * creditCardTransactionRepository.findByInstallmentGroupId(_)
        1 * creditCardTransactionRepository.delete(1L)
    }

    def "execute with includeFuture=true deletes this and every later installment in the group"() {
        given:
        def installment2 = buildTransaction(2L, 2, 3, LocalDate.of(2026, 3, 1), "group-1")
        creditCardTransactionRepository.findById(2L) >> installment2
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> [
                buildTransaction(1L, 1, 3, LocalDate.of(2026, 2, 1), "group-1"),
                installment2,
                buildTransaction(3L, 3, 3, LocalDate.of(2026, 4, 1), "group-1"),
        ]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 4, 1)) >> null

        when:
        service.execute(2L, true)

        then:
        0 * creditCardTransactionRepository.delete(1L)
        1 * creditCardTransactionRepository.delete(2L)
        1 * creditCardTransactionRepository.delete(3L)
    }

    def "execute with includeFuture=true rejects the whole batch when a later installment's invoice is already paid"() {
        given:
        def installment1 = buildTransaction(1L, 1, 3, LocalDate.of(2026, 2, 1), "group-1")
        creditCardTransactionRepository.findById(1L) >> installment1
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> [
                installment1,
                buildTransaction(2L, 2, 3, LocalDate.of(2026, 3, 1), "group-1"),
                buildTransaction(3L, 3, 3, LocalDate.of(2026, 4, 1), "group-1"),
        ]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 2, 1)) >> null
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)

        when:
        service.execute(1L, true)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.delete(_)
    }
}
