package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse
import com.devhouse.financial_plan.application.creditcard.dto.UpdateCreditCardRequest
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    UpdateCreditCardService service = new UpdateCreditCardService(creditCardRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    def "execute updates name, limit, closingDay and dueDay"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        creditCardRepository.update(_) >> { CreditCard c -> c }
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "Itaú", new BigDecimal("8000.00"), 5, 12)

        when:
        CreditCardResponse response = service.execute(10L, request)

        then:
        response.name() == "Itaú"
        response.limit() == new BigDecimal("8000.00")
        response.closingDay() == 5
        response.dueDay() == 12
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "", new BigDecimal("8000.00"), 5, 12)

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(99, "Itaú", new BigDecimal("8000.00"), 5, 12)

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * creditCardRepository.update(_)
    }
}
