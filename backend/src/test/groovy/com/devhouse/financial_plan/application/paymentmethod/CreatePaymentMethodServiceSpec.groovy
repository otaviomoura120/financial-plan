package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.application.paymentmethod.dto.CreatePaymentMethodRequest
import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreatePaymentMethodServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CreatePaymentMethodService service = new CreatePaymentMethodService(paymentMethodRepository, spaceRepository)

    def "execute creates payment method linked to space"() {
        given:
        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest(1L, "Credit Card")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        PaymentMethod savedMethod = new PaymentMethod(7L, null, space, "Credit Card", true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        paymentMethodRepository.save(_) >> savedMethod

        when:
        PaymentMethodResponse response = service.execute(request)

        then:
        response.id() == 7L
        response.name() == "Credit Card"
        response.active()
    }

    def "execute throws DomainException when space not found"() {
        given:
        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest(99L, "Credit Card")
        spaceRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * paymentMethodRepository.save(_)
    }

    def "execute throws DomainException when name is blank"() {
        given:
        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest(1L, "")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * paymentMethodRepository.save(_)
    }
}
