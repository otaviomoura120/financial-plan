package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCreditCardStatusServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    UpdateCreditCardStatusService service = new UpdateCreditCardStatusService(creditCardRepository)

    private CreditCard buildCreditCard(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, active, Instant.now(), null)
    }

    def "execute activates an inactive credit card"() {
        given:
        CreditCard creditCard = buildCreditCard(false)
        creditCardRepository.findById(10L) >> creditCard

        when:
        CreditCardResponse response = service.execute(10L, true)

        then:
        response.active()
        1 * creditCardRepository.update({ it.isActive() }) >> { CreditCard c -> c }
    }

    def "execute deactivates an active credit card"() {
        given:
        CreditCard creditCard = buildCreditCard(true)
        creditCardRepository.findById(10L) >> creditCard

        when:
        CreditCardResponse response = service.execute(10L, false)

        then:
        !response.active()
        1 * creditCardRepository.update({ !it.isActive() }) >> { CreditCard c -> c }
    }
}
