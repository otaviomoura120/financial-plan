package com.devhouse.financial_plan.application.paymentmethod

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

class DeletePaymentMethodServiceSpec extends Specification {

    PaymentMethodRepository paymentMethodRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeletePaymentMethodService service = new DeletePaymentMethodService(paymentMethodRepository, transactionRepository)

    def "execute hard-deletes the payment method when there are no linked transactions"() {
        given:
        transactionRepository.existsByPaymentMethodId(10L) >> false

        when:
        service.execute(10L)

        then:
        1 * paymentMethodRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are linked transactions"() {
        given:
        transactionRepository.existsByPaymentMethodId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * paymentMethodRepository.delete(_)
    }
}
