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
    ListCreditCardTransactionsService service = new ListCreditCardTransactionsService(creditCardTransactionRepository)

    def "execute returns the credit card transactions matching the filter"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        CreditCardTransaction transaction = new CreditCardTransaction(1L, 0, creditCard, user, category, null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", LocalDate.of(2026, 3, 1),
                "group-1", 1, 1, false, null, Instant.now(), null)
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null) >> [transaction]

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null)

        then:
        responses.size() == 1
        responses[0].id() == 1L
        responses[0].creditCardId() == 10L
        responses[0].referenceMonth() == LocalDate.of(2026, 3, 1)
    }

    def "execute returns an empty list when there are no matches"() {
        given:
        creditCardTransactionRepository.findByFilter(99L, null, null, null, null, null, null, null) >> []

        when:
        List<CreditCardTransactionResponse> responses = service.execute(99L, null, null, null, null, null, null)

        then:
        responses.isEmpty()
    }

    def "execute filters by referenceMonth when provided"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        CreditCardTransaction transaction = new CreditCardTransaction(1L, 0, creditCard, user, category, null,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 5), "desc", referenceMonth,
                "group-1", 1, 1, false, null, Instant.now(), null)

        when:
        List<CreditCardTransactionResponse> responses = service.execute(1L, 10L, null, null, null, null, referenceMonth)

        then:
        1 * creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null, null, referenceMonth) >> [transaction]
        responses.size() == 1
        responses[0].referenceMonth() == referenceMonth
    }
}
