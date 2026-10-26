package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.domain.exception.NotFoundException
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant

class DeleteCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    DeleteCreditCardService service = new DeleteCreditCardService(creditCardRepository, creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    def setup() {
        creditCardRepository.findById(10L) >> buildCreditCard(10L, 1L)
    }

    private CreditCard buildCreditCard(Long id, Long spaceId) {
        Space space = new Space(spaceId, 0, "Space " + spaceId, null, Instant.now(), null)
        new CreditCard(id, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    def "execute hard-deletes the credit card when there are no transactions or paid invoices linked to it"() {
        given:
        creditCardTransactionRepository.existsByCreditCardId(10L) >> false
        creditCardInvoicePaymentRepository.existsByCreditCardId(10L) >> false

        when:
        service.execute(1L, 10L)

        then:
        1 * creditCardRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are transactions linked to it"() {
        given:
        creditCardTransactionRepository.existsByCreditCardId(10L) >> true

        when:
        service.execute(1L, 10L)

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
        service.execute(1L, 10L)

        then:
        thrown(DomainException)
        0 * creditCardRepository.delete(_)
    }

    def "execute throws DomainException and does not delete when the credit card belongs to another space"() {
        when:
        service.execute(2L, 10L)

        then:
        NotFoundException exception = thrown()
        exception.message == "Credit card not found"
        0 * creditCardRepository.delete(_)
    }
}
