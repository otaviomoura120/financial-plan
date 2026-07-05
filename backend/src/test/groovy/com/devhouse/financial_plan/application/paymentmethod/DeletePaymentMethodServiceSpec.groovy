package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import spock.lang.Specification

import java.time.Instant

class DeletePaymentMethodServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    DeletePaymentMethodService service = new DeletePaymentMethodService(paymentMethodRepository)

    def "execute deactivates the payment method instead of hard-deleting it"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(10L, 0, space, "Credit Card", true, Instant.now(), null)
        paymentMethodRepository.findById(10L) >> paymentMethod

        when:
        service.execute(10L)

        then:
        1 * paymentMethodRepository.update({ !it.isActive() })
    }
}
