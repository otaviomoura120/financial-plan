package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import spock.lang.Specification

import java.time.Instant

class UpdatePaymentMethodStatusServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    UpdatePaymentMethodStatusService service = new UpdatePaymentMethodStatusService(paymentMethodRepository)

    private PaymentMethod buildPaymentMethod(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new PaymentMethod(10L, 0, space, "Credit Card", active, Instant.now(), null)
    }

    def "execute activates an inactive payment method"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod(false)
        paymentMethodRepository.findById(10L) >> paymentMethod

        when:
        PaymentMethodResponse response = service.execute(10L, true)

        then:
        response.active()
        1 * paymentMethodRepository.update({ it.isActive() }) >> { PaymentMethod p -> p }
    }

    def "execute deactivates an active payment method"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod(true)
        paymentMethodRepository.findById(10L) >> paymentMethod

        when:
        PaymentMethodResponse response = service.execute(10L, false)

        then:
        !response.active()
        1 * paymentMethodRepository.update({ !it.isActive() }) >> { PaymentMethod p -> p }
    }
}
