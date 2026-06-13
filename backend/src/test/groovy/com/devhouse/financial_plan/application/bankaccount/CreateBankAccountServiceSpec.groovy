package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse
import com.devhouse.financial_plan.application.bankaccount.dto.CreateBankAccountRequest
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreateBankAccountServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CreateBankAccountService service = new CreateBankAccountService(bankAccountRepository, spaceRepository)

    def "execute creates bank account linked to space"() {
        given:
        CreateBankAccountRequest request = new CreateBankAccountRequest(1L, "Main Account", "BankCorp", new BigDecimal("1000.00"))
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        BankAccount savedAccount = new BankAccount(10L, 0, space, "Main Account", "BankCorp",
                new BigDecimal("1000.00"), true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        bankAccountRepository.save(_) >> savedAccount

        when:
        BankAccountResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.spaceId() == 1L
        response.name() == "Main Account"
        response.bankName() == "BankCorp"
        response.active()
    }

    def "execute throws DomainException when space not found"() {
        given:
        CreateBankAccountRequest request = new CreateBankAccountRequest(99L, "Main Account", "BankCorp", new BigDecimal("500.00"))
        spaceRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.save(_)
    }

    def "execute throws DomainException when account name is blank"() {
        given:
        CreateBankAccountRequest request = new CreateBankAccountRequest(1L, "", "BankCorp", new BigDecimal("500.00"))
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.save(_)
    }
}
