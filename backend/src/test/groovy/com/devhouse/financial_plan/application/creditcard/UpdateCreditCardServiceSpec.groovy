package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse
import com.devhouse.financial_plan.application.creditcard.dto.UpdateCreditCardRequest
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    UpdateCreditCardService service = new UpdateCreditCardService(creditCardRepository, bankAccountRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    def "execute updates name, limit, closingDay and dueDay"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        creditCardRepository.update(_) >> { CreditCard c -> c }
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "Itaú", new BigDecimal("8000.00"), 5, 12, null)

        when:
        CreditCardResponse response = service.execute(10L, request)

        then:
        response.name() == "Itaú"
        response.limit() == new BigDecimal("8000.00")
        response.closingDay() == 5
        response.dueDay() == 12
        response.bankAccountId() == null
    }

    def "execute links the credit card to a bank account of the same space"() {
        given:
        CreditCard creditCard = buildCreditCard()
        BankAccount bankAccount = new BankAccount(5L, 0, creditCard.getSpace(), "Conta Corrente", "Nubank", BigDecimal.ZERO, true, Instant.now(), null)
        creditCardRepository.findById(10L) >> creditCard
        bankAccountRepository.findById(5L) >> bankAccount
        creditCardRepository.update(_) >> { CreditCard c -> c }
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "Nubank", new BigDecimal("5000.00"), 10, 17, 5L)

        when:
        CreditCardResponse response = service.execute(10L, request)

        then:
        response.bankAccountId() == 5L
        response.bankAccountName() == "Conta Corrente"
    }

    def "execute throws DomainException when the bank account is not found"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        bankAccountRepository.findById(99L) >> null
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "Nubank", new BigDecimal("5000.00"), 10, 17, 99L)

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.update(_)
    }

    def "execute throws DomainException when the bank account belongs to another space"() {
        given:
        CreditCard creditCard = buildCreditCard()
        Space otherSpace = new Space(2L, 0, "Other Space", null, Instant.now(), null)
        BankAccount bankAccount = new BankAccount(5L, 0, otherSpace, "Conta Corrente", "Nubank", BigDecimal.ZERO, true, Instant.now(), null)
        creditCardRepository.findById(10L) >> creditCard
        bankAccountRepository.findById(5L) >> bankAccount
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "Nubank", new BigDecimal("5000.00"), 10, 17, 5L)

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.update(_)
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(0, "", new BigDecimal("8000.00"), 5, 12, null)

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
        UpdateCreditCardRequest request = new UpdateCreditCardRequest(99, "Itaú", new BigDecimal("8000.00"), 5, 12, null)

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * creditCardRepository.update(_)
    }
}
