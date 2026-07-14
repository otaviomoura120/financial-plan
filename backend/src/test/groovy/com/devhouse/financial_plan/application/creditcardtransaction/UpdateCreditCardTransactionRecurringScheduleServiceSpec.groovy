package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringScheduleRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateCreditCardTransactionRecurringScheduleServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()

    UpdateCreditCardTransactionRecurringScheduleService service = new UpdateCreditCardTransactionRecurringScheduleService(
            creditCardTransactionRecurringRepository)

    private CreditCardTransactionRecurring buildRecurring() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(20L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        new CreditCardTransactionRecurring(10L, 0, creditCard, user, category, null, "Netflix", new BigDecimal("39.90"),
                LocalDate.of(2026, 3, 10), true, Instant.now(), null)
    }

    def "execute updates startDate without touching the other fields"() {
        given:
        creditCardTransactionRecurringRepository.findById(10L) >> buildRecurring()
        creditCardTransactionRecurringRepository.update(_) >> { CreditCardTransactionRecurring r -> r }
        UpdateCreditCardTransactionRecurringScheduleRequest request = new UpdateCreditCardTransactionRecurringScheduleRequest(0,
                LocalDate.of(2026, 6, 1))

        when:
        CreditCardTransactionRecurringResponse response = service.execute(10L, request)

        then:
        response.startDate() == LocalDate.of(2026, 6, 1)
        response.description() == "Netflix"
        response.defaultAmount() == new BigDecimal("39.90")
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
