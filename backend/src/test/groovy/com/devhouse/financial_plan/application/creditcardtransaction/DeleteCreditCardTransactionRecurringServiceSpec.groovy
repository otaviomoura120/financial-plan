package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class DeleteCreditCardTransactionRecurringServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    DeleteCreditCardTransactionRecurringService service = new DeleteCreditCardTransactionRecurringService(
            creditCardTransactionRecurringRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(20L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private CreditCardTransactionRecurring buildRecurring() {
        new CreditCardTransactionRecurring(10L, 0, buildCreditCard(), buildUser(),
                new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null), null, "Netflix",
                new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private CreditCardTransaction buildGeneratedTransaction(Long id, CreditCardTransactionRecurring recurring, LocalDate referenceMonth) {
        new CreditCardTransaction(id, 0, buildCreditCard(), recurring, buildUser(),
                new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null), null, new BigDecimal("39.90"),
                false, referenceMonth, "Netflix", referenceMonth, "group-1", 1, 1, false, null, Instant.now(), null)
    }

    def "execute deletes transactions from the current month onward and detaches (keeps) past transactions"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()
        creditCardTransactionRecurringRepository.findById(10L) >> recurring
        CreditCardTransaction pastTransaction = buildGeneratedTransaction(1L, recurring, YearMonth.now().minusMonths(2).atDay(1))
        CreditCardTransaction currentMonthTransaction = buildGeneratedTransaction(2L, recurring, YearMonth.now().atDay(1))
        CreditCardTransaction futureTransaction = buildGeneratedTransaction(3L, recurring, YearMonth.now().plusMonths(3).atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [pastTransaction, currentMonthTransaction, futureTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null

        when:
        service.execute(10L)

        then:
        pastTransaction.getCreditCardTransactionRecurring() == null
        1 * creditCardTransactionRepository.update(pastTransaction)
        0 * creditCardTransactionRepository.update(currentMonthTransaction)
        0 * creditCardTransactionRepository.update(futureTransaction)
        1 * creditCardTransactionRepository.delete(2L)
        1 * creditCardTransactionRepository.delete(3L)
        0 * creditCardTransactionRepository.delete(1L)
        1 * creditCardTransactionRecurringRepository.delete(10L)
    }

    def "execute detaches (does not delete) a current/future transaction whose invoice is already paid"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring()
        creditCardTransactionRecurringRepository.findById(10L) >> recurring
        CreditCardTransaction paidFutureTransaction = buildGeneratedTransaction(1L, recurring, YearMonth.now().atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [paidFutureTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, YearMonth.now().atDay(1)) >> Mock(CreditCardInvoicePayment)

        when:
        service.execute(10L)

        then:
        paidFutureTransaction.getCreditCardTransactionRecurring() == null
        1 * creditCardTransactionRepository.update(paidFutureTransaction)
        0 * creditCardTransactionRepository.delete(1L)
        1 * creditCardTransactionRecurringRepository.delete(10L)
    }

    def "execute hard-deletes the recurring when it has no generated transactions"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []

        when:
        service.execute(10L)

        then:
        0 * creditCardTransactionRepository.update(_)
        0 * creditCardTransactionRepository.delete(_)
        1 * creditCardTransactionRecurringRepository.delete(10L)
    }

    def "execute throws DomainException when recurring does not exist"() {
        given:
        creditCardTransactionRecurringRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
        0 * creditCardTransactionRepository.delete(_)
        0 * creditCardTransactionRecurringRepository.delete(_)
    }
}
