package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class UpdateCreditCardTransactionRecurringServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    UpdateCreditCardTransactionRecurringService service = new UpdateCreditCardTransactionRecurringService(
            creditCardTransactionRecurringRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository,
            categoryRepository, subCategoryRepository)

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

    def "execute updates category, defaultAmount and startDate"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        Category category = new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        categoryRepository.findById(31L) >> category
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 6, 1))

        when:
        CreditCardTransactionRecurringResponse response = service.execute(10L, request)

        then:
        response.categoryId() == 31L
        response.defaultAmount() == new BigDecimal("59.90")
        response.description() == "Xbox Game Pass"
        response.startDate() == LocalDate.of(2026, 6, 1)
    }

    def "execute also updates already-generated transactions from the current month onward"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        Category newCategory = new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        categoryRepository.findById(31L) >> newCategory
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        CreditCardTransaction pastTransaction = buildGeneratedTransaction(1L, YearMonth.now().minusMonths(2).atDay(10), YearMonth.now().minusMonths(2).atDay(1))
        CreditCardTransaction currentMonthTransaction = buildGeneratedTransaction(2L, YearMonth.now().atDay(10), YearMonth.now().atDay(1))
        CreditCardTransaction futureTransaction = buildGeneratedTransaction(3L, YearMonth.now().plusMonths(3).atDay(10), YearMonth.now().plusMonths(3).atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [pastTransaction, currentMonthTransaction, futureTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 3, 10))

        when:
        service.execute(10L, request)

        then:
        currentMonthTransaction.getCategory() == newCategory
        currentMonthTransaction.getAmount() == new BigDecimal("59.90")
        currentMonthTransaction.getDescription() == "Xbox Game Pass"
        futureTransaction.getCategory() == newCategory
        futureTransaction.getAmount() == new BigDecimal("59.90")
        pastTransaction.getCategory() != newCategory
        pastTransaction.getAmount() == new BigDecimal("39.90")
        1 * creditCardTransactionRepository.update(currentMonthTransaction)
        1 * creditCardTransactionRepository.update(futureTransaction)
        0 * creditCardTransactionRepository.update(pastTransaction)
    }

    def "execute recalculates purchaseDate for pending transactions from the current month onward when startDate changes"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        categoryRepository.findById(31L) >> new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth pastMonth = YearMonth.now().minusMonths(2)
        YearMonth currentMonth = YearMonth.now()
        YearMonth futureMonth = YearMonth.now().plusMonths(3)
        CreditCardTransaction pastTransaction = buildGeneratedTransaction(1L, pastMonth.atDay(10), pastMonth.atDay(1))
        CreditCardTransaction currentMonthTransaction = buildGeneratedTransaction(2L, currentMonth.atDay(10), currentMonth.atDay(1))
        CreditCardTransaction futureTransaction = buildGeneratedTransaction(3L, futureMonth.atDay(10), futureMonth.atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [pastTransaction, currentMonthTransaction, futureTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("39.90"), "Netflix", LocalDate.of(2026, 1, 15))

        when:
        service.execute(10L, request)

        then:
        currentMonthTransaction.getPurchaseDate() == currentMonth.atDay(15)
        currentMonthTransaction.getReferenceMonth() == currentMonth.atDay(1)
        futureTransaction.getPurchaseDate() == futureMonth.atDay(15)
        futureTransaction.getReferenceMonth() == futureMonth.atDay(1)
        pastTransaction.getPurchaseDate() == pastMonth.atDay(10)
    }

    def "execute recomputes referenceMonth via the closing day when the new anchor day crosses the closing boundary"() {
        given:
        CreditCard creditCard = buildCreditCard(5, 15)
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring(creditCard)
        categoryRepository.findById(31L) >> new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction transaction = buildGeneratedTransaction(1L, currentMonth.atDay(3), currentMonth.atDay(1), creditCard)
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [transaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("39.90"), "Netflix", LocalDate.of(2026, 1, 10))

        when:
        service.execute(10L, request)

        then:
        transaction.getPurchaseDate() == currentMonth.atDay(10)
        transaction.getReferenceMonth() == currentMonth.plusMonths(1).atDay(1)
    }

    def "execute does not move a transaction into a destination invoice that is already paid"() {
        given:
        CreditCard creditCard = buildCreditCard(5, 15)
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring(creditCard)
        categoryRepository.findById(31L) >> new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction transaction = buildGeneratedTransaction(1L, currentMonth.atDay(3), currentMonth.atDay(1), creditCard)
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [transaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.atDay(1)) >> null
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.plusMonths(1).atDay(1)) >> Mock(CreditCardInvoicePayment)
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("39.90"), "Netflix", LocalDate.of(2026, 1, 10))

        when:
        service.execute(10L, request)

        then:
        transaction.getPurchaseDate() == currentMonth.atDay(3)
        transaction.getReferenceMonth() == currentMonth.atDay(1)
        0 * creditCardTransactionRepository.update(transaction)
    }

    def "execute does not update a current/future transaction whose invoice is already paid"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        Category newCategory = new Category(31L, 0, null, "Lazer", true, Instant.now(), null)
        categoryRepository.findById(31L) >> newCategory
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        YearMonth currentMonth = YearMonth.now()
        CreditCardTransaction paidTransaction = buildGeneratedTransaction(1L, currentMonth.atDay(10), currentMonth.atDay(1))
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [paidTransaction]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, currentMonth.atDay(1)) >> Mock(CreditCardInvoicePayment)
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 3, 15))

        when:
        service.execute(10L, request)

        then:
        paidTransaction.getCategory() != newCategory
        paidTransaction.getPurchaseDate() == currentMonth.atDay(10)
        0 * creditCardTransactionRepository.update(paidTransaction)
    }

    def "execute throws DomainException when recurring does not exist"() {
        given:
        creditCardTransactionRecurringRepository.findById(99L) >> null
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 3, 10))

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRecurringRepository.update(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        categoryRepository.findById(31L) >> null
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(0, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 3, 10))

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRecurringRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when version does not match"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        UpdateCreditCardTransactionRecurringRequest request = new UpdateCreditCardTransactionRecurringRequest(99, 31L, null,
                new BigDecimal("59.90"), "Xbox Game Pass", LocalDate.of(2026, 3, 10))

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * creditCardTransactionRecurringRepository.update(_)
    }
}
