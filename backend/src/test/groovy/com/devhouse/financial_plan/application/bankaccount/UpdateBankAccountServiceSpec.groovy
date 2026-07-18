package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse
import com.devhouse.financial_plan.application.bankaccount.dto.UpdateBankAccountRequest
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import spock.lang.Specification

import java.time.Instant

class UpdateBankAccountServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    UpdateBankAccountService service = new UpdateBankAccountService(bankAccountRepository)

    private BankAccount buildAccount() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(10L, 0, space, "Main Account", "BankCorp", new BigDecimal("500.00"), true, Instant.now(), null)
    }

    def "execute updates name and bankName, keeping balance untouched"() {
        given:
        BankAccount account = buildAccount()
        bankAccountRepository.findById(10L) >> account
        bankAccountRepository.update(_) >> { BankAccount a -> a }
        UpdateBankAccountRequest request = new UpdateBankAccountRequest(0, "New Name", "New Bank")

        when:
        BankAccountResponse response = service.execute(10L, request)

        then:
        response.name() == "New Name"
        response.bankName() == "New Bank"
        response.balance() == new BigDecimal("500.00")
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        BankAccount account = buildAccount()
        bankAccountRepository.findById(10L) >> account
        UpdateBankAccountRequest request = new UpdateBankAccountRequest(0, "", "New Bank")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        BankAccount account = buildAccount()
        bankAccountRepository.findById(10L) >> account
        UpdateBankAccountRequest request = new UpdateBankAccountRequest(99, "New Name", "New Bank")

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * bankAccountRepository.update(_)
    }
}
