package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class EnsureRecurringCreditCardTransactionsGeneratedServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    EnsureRecurringCreditCardTransactionsGeneratedService service = new EnsureRecurringCreditCardTransactionsGeneratedService(
            creditCardTransactionRecurringRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private CreditCard buildCreditCard(int closingDay = 10, int dueDay = 17) {
        new CreditCard(20L, 0, buildSpace(), null, "Nubank", new BigDecimal("5000.00"), closingDay, dueDay, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category buildCategory() {
        new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
    }

    private CreditCardTransactionRecurring buildRecurring(LocalDate startDate, boolean active = true, CreditCard creditCard = buildCreditCard()) {
        new CreditCardTransactionRecurring(10L, 0, creditCard, buildUser(), buildCategory(), null, "Netflix",
                new BigDecimal("39.90"), startDate, active, Instant.now(), null)
    }

    private CreditCardTransaction buildGeneratedTransaction(LocalDate purchaseDate, LocalDate referenceMonth) {
        new CreditCardTransaction(1L, 0, buildCreditCard(), buildRecurring(purchaseDate), buildUser(), buildCategory(), null,
                new BigDecimal("39.90"), purchaseDate, "Netflix", referenceMonth, "group-1", 1, 1, false, null, Instant.now(), null)
    }

    def "execute generates through at least 6 months ahead of the current month, even when the requested upToDate is only the current month"() {
        given:
        YearMonth startMonth = YearMonth.now().minusMonths(2)
        CreditCardTransactionRecurring recurring = buildRecurring(startMonth.atDay(10))
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, LocalDate.now())

        then:
        YearMonth expectedCapMonth = YearMonth.now().plusMonths(6)
        saved.size() == ChronoUnit.MONTHS.between(startMonth, expectedCapMonth) + 1
        YearMonth.from(saved.first().purchaseDate) == startMonth
        YearMonth.from(saved.last().purchaseDate) == expectedCapMonth
        saved.every { it.installmentNumber == 1 && it.totalInstallments == 1 }
        saved*.installmentGroupId.toSet().size() == saved.size()
    }

    def "execute extends beyond the 6-month floor when the requested upToDate asks for more"() {
        given:
        YearMonth startMonth = YearMonth.now()
        CreditCardTransactionRecurring recurring = buildRecurring(startMonth.atDay(10))
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, YearMonth.now().plusMonths(9).atDay(1))

        then:
        YearMonth.from(saved.last().purchaseDate) == YearMonth.now().plusMonths(9)
    }

    def "execute computes referenceMonth via the credit card closing day, not just the purchase month"() {
        given:
        // closingDay = 5: a purchase on day 10 (after closing) rolls into next month's invoice
        CreditCard creditCard = buildCreditCard(5, 15)
        YearMonth startMonth = YearMonth.now()
        CreditCardTransactionRecurring recurring = buildRecurring(startMonth.atDay(10), true, creditCard)
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, LocalDate.now())

        then:
        CreditCardTransaction firstGenerated = saved.find { it.purchaseDate == startMonth.atDay(10) }
        firstGenerated.referenceMonth == startMonth.plusMonths(1).atDay(1)
    }

    def "execute only generates months after the last already-generated purchase month"() {
        given:
        YearMonth startMonth = YearMonth.now().minusMonths(3)
        CreditCardTransactionRecurring recurring = buildRecurring(startMonth.atDay(10))
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> [
                buildGeneratedTransaction(startMonth.atDay(10), startMonth.atDay(1)),
                buildGeneratedTransaction(startMonth.plusMonths(1).atDay(10), startMonth.plusMonths(1).atDay(1))
        ]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, LocalDate.now())

        then:
        YearMonth.from(saved.first().purchaseDate) == startMonth.plusMonths(2)
        saved.every { !YearMonth.from(it.purchaseDate).isBefore(startMonth.plusMonths(2)) }
    }

    def "execute is idempotent: does not recreate months that already have a generated transaction"() {
        given:
        YearMonth startMonth = YearMonth.now()
        CreditCardTransactionRecurring recurring = buildRecurring(startMonth.atDay(10))
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >>
                [buildGeneratedTransaction(startMonth.atDay(10), startMonth.atDay(1))]
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null

        when:
        service.execute(1L, LocalDate.now())

        then:
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute clamps the anchor day to the target month's length for short months"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring(LocalDate.of(2021, 1, 31))
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, LocalDate.of(2021, 2, 15))

        then:
        saved.find { YearMonth.from(it.purchaseDate) == YearMonth.of(2021, 1) }.purchaseDate == LocalDate.of(2021, 1, 31)
        saved.find { YearMonth.from(it.purchaseDate) == YearMonth.of(2021, 2) }.purchaseDate == LocalDate.of(2021, 2, 28)
    }

    def "execute skips a month whose invoice is already paid, without throwing, and still generates other months"() {
        given:
        CreditCard creditCard = buildCreditCard(15, 17)
        CreditCardTransactionRecurring recurring = buildRecurring(LocalDate.of(2021, 1, 10), true, creditCard)
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]
        creditCardTransactionRepository.findByCreditCardTransactionRecurringId(10L) >> []
        creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(10L, _) >> []
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(20L, LocalDate.of(2021, 2, 1)) >> Mock(CreditCardInvoicePayment)
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        when:
        service.execute(1L, LocalDate.of(2021, 3, 15))

        then:
        noExceptionThrown()
        saved.find { it.purchaseDate == LocalDate.of(2021, 1, 10) } != null
        saved.find { it.purchaseDate == LocalDate.of(2021, 3, 10) } != null
        saved.find { YearMonth.from(it.purchaseDate) == YearMonth.of(2021, 2) } == null
    }

    def "execute skips recurring templates that are inactive"() {
        given:
        CreditCardTransactionRecurring recurring = buildRecurring(LocalDate.of(2026, 1, 10), false)
        creditCardTransactionRecurringRepository.findBySpaceId(1L) >> [recurring]

        when:
        service.execute(1L, LocalDate.of(2026, 3, 15))

        then:
        0 * creditCardTransactionRepository.findByCreditCardTransactionRecurringId(_)
        0 * creditCardTransactionRepository.save(_)
    }
}
