package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringScheduleRequest
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

class UpdateCreditCardTransactionRecurringScheduleServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    UpdateCreditCardTransactionRecurringScheduleService service = new UpdateCreditCardTransactionRecurringScheduleService(
            creditCardTransactionRecurringRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard(int closingDay = 28, int dueDay = 5) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(20L, 0, space, null, "Nubank", new BigDecimal("5000.00"), closingDay, dueDay, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private CreditCardTransactionRecurring buildRecurring(CreditCard creditCard = buildCreditCard()) {
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        new CreditCardTransactionRecurring(10L, 0, creditCard, buildUser(), category, null, "Netflix", new BigDecimal("39.90"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    private CreditCardTransaction buildGeneratedTransaction(Long id, LocalDate purchaseDate, LocalDate referenceMonth, CreditCard creditCard = buildCreditCard()) {
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        new CreditCardTransaction(id, 0, creditCard, buildRecurring(creditCard), buildUser(), category, null,
                new BigDecimal("39.90"), purchaseDate, "Netflix", referenceMonth, "group-1", 1, 1, false, null, Instant.now(), null)
    }

    def "execute updates startDate without touching the other fields"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 6, 1))

        when:
        CreditCardTransactionRecurringResponse response = service.execute(10L, request)

        then:
        response.startDate() == LocalDate.of(2026, 6, 1)
        response.description() == "Netflix"
        response.defaultAmount() == new BigDecimal("39.90")
    }

    def "execute recalculates purchaseDate for pending transactions from the current month onward, keeping the same referenceMonth when the closing day is not crossed"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth pastMonth = YearMonth.now().minusMonths(2)
        YearMonth currentMonth = YearMonth.now()
        YearMonth futureMonth = YearMonth.now().plusMonths(3)
        CreditCardTransaction pastTransaction = buildGeneratedTransaction(1L, pastMonth.atDay(10), pastMonth.atDay(1))
        CreditCardTransaction currentMonthTransaction = buildGeneratedTransaction(2L, currentMonth.atDay(10), currentMonth.atDay(1))
        CreditCardTransaction futureTransaction = buildGeneratedTransaction(3L, futureMonth.atDay(10), futureMonth.atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [pastTransaction, currentMonthTransaction, futureTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 1, 15))

        when:
        service.execute(10L, request)

        then:
        currentMonthTransaction.getPurchaseDate() == currentMonth.atDay(15)
        currentMonthTransaction.getReferenceMonth() == currentMonth.atDay(1)
        futureTransaction.getPurchaseDate() == futureMonth.atDay(15)
        futureTransaction.getReferenceMonth() == futureMonth.atDay(1)
        pastTransaction.getPurchaseDate() == pastMonth.atDay(10)
        1 * creditCardTransactionRepository.update(currentMonthTransaction)
        1 * creditCardTransactionRepository.update(futureTransaction)
        0 * creditCardTransactionRepository.update(pastTransaction)
    }

    def "execute recomputes referenceMonth via the closing day when the new anchor day crosses the closing boundary"() {
        given:
        CreditCard creditCard = buildCreditCard(5, 15)
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring(creditCard)
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction transaction = buildGeneratedTransaction(1L, currentMonth.atDay(3), currentMonth.atDay(1), creditCard)
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [transaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 1, 10))

        when:
        service.execute(10L, request)

        then:
        transaction.getPurchaseDate() == currentMonth.atDay(10)
        transaction.getReferenceMonth() == currentMonth.plusMonths(1).atDay(1)
        1 * creditCardTransactionRepository.update(transaction)
    }

    def "execute does not touch a transaction whose invoice is already paid"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction paidTransaction = buildGeneratedTransaction(1L, currentMonth.atDay(10), currentMonth.atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [paidTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.atDay(1)) >> Mock(CreditCardInvoicePayment)
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 1, 15))

        when:
        service.execute(10L, request)

        then:
        paidTransaction.getPurchaseDate() == currentMonth.atDay(10)
        0 * creditCardTransactionRepository.update(paidTransaction)
    }

    def "execute does not move a transaction into a destination invoice that is already paid"() {
        given:
        CreditCard creditCard = buildCreditCard(5, 15)
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring(creditCard)
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction transaction = buildGeneratedTransaction(1L, currentMonth.atDay(3), currentMonth.atDay(1), creditCard)
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [transaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.atDay(1)) >> null
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.plusMonths(1).atDay(1)) >> Mock(CreditCardInvoicePayment)
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 1, 10))

        when:
        service.execute(10L, request)

        then:
        transaction.getPurchaseDate() == currentMonth.atDay(3)
        transaction.getReferenceMonth() == currentMonth.atDay(1)
        0 * creditCardTransactionRepository.update(transaction)
    }

    def "execute throws DomainException when recurring does not exist"() {
        given:
        creditCardTransactionRecurringRepository.findById(99L) >> null
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 6, 1))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRecurringRepository.update(_)
    }
}
