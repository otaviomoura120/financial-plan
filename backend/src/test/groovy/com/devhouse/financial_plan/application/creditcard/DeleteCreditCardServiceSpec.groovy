package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

class DeleteCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    DeleteCreditCardService service = new DeleteCreditCardService(creditCardRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    def "execute hard-deletes the credit card when there are no transactions or paid invoices linked to it"() {
        given:
        creditCardTransactionRepository.existsByCreditCardId(10L) >> false
        creditCardInvoicePaymentRepository.existsByCreditCardId(10L) >> false

        when:
        service.execute(10L)

        then:
        1 * creditCardRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are transactions linked to it"() {
        given:
        creditCardTransactionRepository.existsByCreditCardId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * creditCardRepository.delete(_)
        0 * creditCardInvoicePaymentRepository.existsByCreditCardId(_)
    }

    def "execute throws DomainException and does not delete when there are paid invoices linked to it"() {
        given:
        creditCardTransactionRepository.existsByCreditCardId(10L) >> false
        creditCardInvoicePaymentRepository.existsByCreditCardId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * creditCardRepository.delete(_)
    }
}
