package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.application.paymentmethod.dto.PaymentMethodResponse
import com.devhouse.financial_plan.application.paymentmethod.dto.UpdatePaymentMethodRequest
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import spock.lang.Specification

import java.time.Instant

class UpdatePaymentMethodServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    UpdatePaymentMethodService service = new UpdatePaymentMethodService(paymentMethodRepository)

    private PaymentMethod buildPaymentMethod() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new PaymentMethod(10L, 0, space, "Credit Card", true, Instant.now(), null)
    }

    def "execute renames the payment method"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod()
        paymentMethodRepository.findById(10L) >> paymentMethod
        paymentMethodRepository.update(_) >> { PaymentMethod pm -> pm }
        UpdatePaymentMethodRequest request = new UpdatePaymentMethodRequest(0, "Debit Card")

        when:
        PaymentMethodResponse response = service.execute(10L, request)

        then:
        response.name() == "Debit Card"
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod()
        paymentMethodRepository.findById(10L) >> paymentMethod
        UpdatePaymentMethodRequest request = new UpdatePaymentMethodRequest(0, "")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * paymentMethodRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        PaymentMethod paymentMethod = buildPaymentMethod()
        paymentMethodRepository.findById(10L) >> paymentMethod
        UpdatePaymentMethodRequest request = new UpdatePaymentMethodRequest(99, "Debit Card")

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * paymentMethodRepository.update(_)
    }
}
