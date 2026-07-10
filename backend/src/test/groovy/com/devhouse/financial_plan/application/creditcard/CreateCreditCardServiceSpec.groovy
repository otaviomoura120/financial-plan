package com.devhouse.financial_plan.application.creditcard

import com.devhouse.financial_plan.application.creditcard.dto.CreateCreditCardRequest
import com.devhouse.financial_plan.application.creditcard.dto.CreditCardResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreateCreditCardServiceSpec extends Specification {

    CreditCardRepository creditCardRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    CreateCreditCardService service = new CreateCreditCardService(creditCardRepository, spaceRepository, bankAccountRepository)

    def "execute creates credit card linked to space"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", new BigDecimal("5000.00"), 10, 17, null)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard savedCreditCard = new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        creditCardRepository.save(_) >> savedCreditCard

        when:
        CreditCardResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.spaceId() == 1L
        response.name() == "Nubank"
        response.limit() == new BigDecimal("5000.00")
        response.closingDay() == 10
        response.dueDay() == 17
        response.active()
        response.bankAccountId() == null
        response.bankAccountName() == null
    }

    def "execute creates credit card linked to a bank account of the same space"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", new BigDecimal("5000.00"), 10, 17, 5L)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        BankAccount bankAccount = new BankAccount(5L, 0, space, "Conta Corrente", "Nubank", BigDecimal.ZERO, true, Instant.now(), null)
        CreditCard savedCreditCard = new CreditCard(10L, 0, space, bankAccount, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        bankAccountRepository.findById(5L) >> bankAccount

        when:
        CreditCardResponse response = service.execute(request)

        then:
        1 * creditCardRepository.save({ CreditCard card -> card.bankAccount.id == 5L }) >> savedCreditCard
        response.bankAccountId() == 5L
        response.bankAccountName() == "Conta Corrente"
    }

    def "execute throws DomainException when bank account is not found"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", new BigDecimal("5000.00"), 10, 17, 99L)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        bankAccountRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.save(_)
    }

    def "execute throws DomainException when bank account belongs to another space"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", new BigDecimal("5000.00"), 10, 17, 5L)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Space otherSpace = new Space(2L, 0, "Other Space", null, Instant.now(), null)
        BankAccount bankAccount = new BankAccount(5L, 0, otherSpace, "Conta Corrente", "Nubank", BigDecimal.ZERO, true, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        bankAccountRepository.findById(5L) >> bankAccount

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.save(_)
    }

    def "execute throws DomainException when space not found"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(99L, "Nubank", new BigDecimal("5000.00"), 10, 17, null)
        spaceRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.save(_)
    }

    def "execute throws DomainException when limit is not positive"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", BigDecimal.ZERO, 10, 17, null)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.save(_)
    }

    def "execute throws DomainException when closingDay/dueDay is out of range"() {
        given:
        CreateCreditCardRequest request = new CreateCreditCardRequest(1L, "Nubank", new BigDecimal("5000.00"), closingDay, dueDay, null)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardRepository.save(_)

        where:
        closingDay | dueDay
        0          | 17
        32         | 17
        10         | 0
        10         | 32
    }
}
