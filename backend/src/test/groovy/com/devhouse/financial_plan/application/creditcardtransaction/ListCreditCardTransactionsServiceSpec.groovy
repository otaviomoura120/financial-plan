package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListCreditCardTransactionsServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService = Mock()
    ListCreditCardTransactionsService service = new ListCreditCardTransactionsService(creditCardTransactionRepository,
            ensureRecurringCreditCardTransactionsGeneratedService)

    private CreditCard creditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User user() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category category() {
        new Category(20L, 0, null, "Food", true, Instant.now(), null)
    }

    def "execute returns the credit card transactions matching the filter"() {
        given:
        CreditCardTransaction transaction = new CreditCardTransaction(1L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> [transaction]

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null)

        then:
        responses.size() == 1
        responses[0].id() == 1L
        responses[0].creditCardId() == 10L
        responses[0].referenceMonth() == LocalDate.of(2026, 3, 1)
        responses[0].competenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "execute returns an empty list when there are no matches"() {
        given:
        creditCardTransactionRepository.findByFilter(99L, null, null, null, null, null) >> []

        when:
        List<CreditCardTransactionResponse> responses = service.execute(99L, null, null, null, null, null, null, null)

        then:
        responses.isEmpty()
    }

    def "execute ensures recurring transactions are generated up to the requested 'to' date before listing"() {
        given:
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> []

        when:
        service.execute(1L, 10L, null, null, null, LocalDate.of(2026, 5, 31), null, null)

        then:
        1 * ensureRecurringCreditCardTransactionsGeneratedService.execute(1L, LocalDate.of(2026, 5, 31))
    }

    def "execute ensures recurring transactions are generated up to today when 'to' is not provided"() {
        given:
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> []

        when:
        service.execute(1L, 10L, null, null, null, null, null, null)

        then:
        1 * ensureRecurringCreditCardTransactionsGeneratedService.execute(1L, LocalDate.now())
    }

    def "execute filters by referenceMonth exactly via the repository"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        CreditCardTransaction transaction = new CreditCardTransaction(1L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", referenceMonth,
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null, null, null, referenceMonth, null)

        then:
        1 * creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, referenceMonth) >> [transaction]
        responses.size() == 1
        responses[0].referenceMonth() == referenceMonth
    }

    def "execute filters by competenceMonth in memory, excluding transactions from other months"() {
        given:
        LocalDate competenceMonth = LocalDate.of(2026, 7, 1)
        CreditCardTransaction matching = new CreditCardTransaction(1L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("100.00"), LocalDate.of(2026, 7, 12), "desc", LocalDate.of(2026, 8, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
        CreditCardTransaction other = new CreditCardTransaction(2L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("50.00"), LocalDate.of(2026, 6, 12), "desc", LocalDate.of(2026, 7, 1),
                "group-2", 1, 1, false, null, Instant.now(), null)
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> [matching, other]

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null, null, null, null, competenceMonth)

        then:
        responses.size() == 1
        responses[0].id() == 1L
        responses[0].competenceMonth() == competenceMonth
        responses[0].referenceMonth() == LocalDate.of(2026, 8, 1)
    }

    def "execute filters by the from/to competenceMonth range in memory"() {
        given:
        CreditCardTransaction inRange = new CreditCardTransaction(1L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("33.33"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
        CreditCardTransaction outOfRange = new CreditCardTransaction(2L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("33.33"), LocalDate.of(2026, 4, 5), "desc", LocalDate.of(2026, 4, 1),
                "group-2", 1, 1, false, null, Instant.now(), null)
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> [inRange, outOfRange]

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null, null)

        then:
        responses.size() == 1
        responses[0].id() == 1L
    }

    def "execute computes totalAmount by summing every installment of the group, once per group"() {
        given:
        CreditCardTransaction installment1 = new CreditCardTransaction(1L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("33.33"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 3, false, null, Instant.now(), null)
        CreditCardTransaction installment2 = new CreditCardTransaction(2L, 0, creditCard(), null, user(), category(), null,
                new BigDecimal("33.33"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 4, 1),
                "group-1", 2, 3, false, null, Instant.now(), null)
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> [installment1, installment2]

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30), null, null)

        then:
        1 * creditCardTransactionRepository.findByInstallmentGroupId("group-1") >>
                [installment1, installment2, new CreditCardTransaction(3L, 0, creditCard(), null, user(), category(), null,
                        new BigDecimal("33.34"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 5, 1),
                        "group-1", 3, 3, false, null, Instant.now(), null)]
        responses.size() == 2
        responses[0].totalAmount() == new BigDecimal("100.00")
        responses[1].totalAmount() == new BigDecimal("100.00")
    }
}
