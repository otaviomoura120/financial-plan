package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import spock.lang.Specification

import java.time.Instant

class ListCreditCardsServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    ListCreditCardsService service = new ListCreditCardsService(creditCardRepository)

    def "execute returns the credit cards of the space"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        creditCardRepository.findBySpaceId(1L) >> [creditCard]

        when:
        List<CreditCardResponse> responses = service.execute(1L)

        then:
        responses.size() == 1
        responses[0].id() == 10L
        responses[0].spaceId() == 1L
        responses[0].name() == "Nubank"
    }

    def "execute returns an empty list when the space has no credit cards"() {
        given:
        creditCardRepository.findBySpaceId(99L) >> []

        when:
        List<CreditCardResponse> responses = service.execute(99L)

        then:
        responses.isEmpty()
    }
}
