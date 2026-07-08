package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import spock.lang.Specification

import java.time.Instant

class DeactivateCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    DeactivateCreditCardService service = new DeactivateCreditCardService(creditCardRepository)

    def "execute deactivates the credit card"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        creditCardRepository.findById(10L) >> creditCard

        when:
        service.execute(10L)

        then:
        !creditCard.isActive()
        1 * creditCardRepository.update({ !it.isActive() })
    }

    def "execute throws DomainException when credit card does not exist"() {
        given:
        creditCardRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * creditCardRepository.update(_)
    }
}
