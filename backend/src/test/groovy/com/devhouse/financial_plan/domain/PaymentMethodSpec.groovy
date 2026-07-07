package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class PaymentMethodSpec extends Specification {

    private PaymentMethod buildPaymentMethod(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new PaymentMethod(10L, 0, space, "Credit Card", active, Instant.now(), null)
    }

    def "validate throws DomainException when space is null"() {
        given:
        PaymentMethod paymentMethod = new PaymentMethod(10L, 0, null, "Credit Card", true, Instant.now(), null)

        when:
        paymentMethod.validate()

        then:
        thrown(DomainException)
    }

    def "deactivate sets active to false"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod(true)

        when:
        paymentMethod.deactivate()

        then:
        !paymentMethod.isActive()
    }

    def "activate sets active to true"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod(false)

        when:
        paymentMethod.activate()

        then:
        paymentMethod.isActive()
    }
}
