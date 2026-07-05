package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import spock.lang.Specification

import java.time.Instant

class ListPaymentMethodsServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    ListPaymentMethodsService service = new ListPaymentMethodsService(paymentMethodRepository)

    def "execute returns the payment methods of the space"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(10L, 0, space, "Credit Card", true, Instant.now(), null)
        paymentMethodRepository.findBySpaceId(1L) >> [paymentMethod]

        when:
        List<PaymentMethodResponse> responses = service.execute(1L)

        then:
        responses.size() == 1
        responses[0].id() == 10L
        responses[0].name() == "Credit Card"
    }

    def "execute returns an empty list when the space has no payment methods"() {
        given:
        paymentMethodRepository.findBySpaceId(99L) >> []

        when:
        List<PaymentMethodResponse> responses = service.execute(99L)

        then:
        responses.isEmpty()
    }
}
