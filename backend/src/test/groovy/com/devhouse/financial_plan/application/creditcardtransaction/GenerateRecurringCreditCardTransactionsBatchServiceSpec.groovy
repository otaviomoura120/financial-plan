package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateRecurringCreditCardTransactionsBatchServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService = Mock()

    GenerateRecurringCreditCardTransactionsBatchService service = new GenerateRecurringCreditCardTransactionsBatchService(
            creditCardTransactionRecurringRepository, ensureRecurringCreditCardTransactionsGeneratedService)

    private CreditCardTransactionRecurring buildRecurring(Long id, Long creditCardId) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(creditCardId, 0, space, null, "Nubank", new BigDecimal("5000.00"),
                10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        new CreditCardTransactionRecurring(id, 0, creditCard, user, category, null, "Netflix",
                new BigDecimal("39.90"), LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "execute tops up every active subscription, across credit cards of different spaces"() {
        given:
        CreditCardTransactionRecurring first = buildRecurring(10L, 20L)
        CreditCardTransactionRecurring second = buildRecurring(11L, 21L)
        creditCardTransactionRecurringRepository.findAllActive() >> [first, second]

        when:
        service.execute()

        then:
        1 * ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(first)
        1 * ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(second)
    }

    def "execute keeps processing the batch when one subscription fails"() {
        given:
        CreditCardTransactionRecurring failing = buildRecurring(10L, 20L)
        CreditCardTransactionRecurring healthy = buildRecurring(11L, 21L)
        creditCardTransactionRecurringRepository.findAllActive() >> [failing, healthy]
        ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(failing) >> {
            throw new IllegalStateException("boom")
        }

        when:
        service.execute()

        then:
        notThrown(Exception)
        1 * ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(healthy)
    }

    def "execute does nothing when there is no active subscription"() {
        given:
        creditCardTransactionRecurringRepository.findAllActive() >> []

        when:
        service.execute()

        then:
        0 * ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(_)
    }
}
