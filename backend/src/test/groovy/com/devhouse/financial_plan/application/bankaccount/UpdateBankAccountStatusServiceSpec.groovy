package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import spock.lang.Specification

import java.time.Instant

class UpdateBankAccountStatusServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    UpdateBankAccountStatusService service = new UpdateBankAccountStatusService(bankAccountRepository)

    private BankAccount buildAccount(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(10L, 0, space, "Main Account", "BankCorp", new BigDecimal("500.00"), active, Instant.now(), null)
    }

    def "execute activates an inactive bank account"() {
        given:
        BankAccount account = buildAccount(false)
        bankAccountRepository.findById(10L) >> account

        when:
        BankAccountResponse response = service.execute(10L, true)

        then:
        response.active()
        1 * bankAccountRepository.update({ it.isActive() }) >> { BankAccount a -> a }
    }

    def "execute deactivates an active bank account"() {
        given:
        BankAccount account = buildAccount(true)
        bankAccountRepository.findById(10L) >> account

        when:
        BankAccountResponse response = service.execute(10L, false)

        then:
        !response.active()
        1 * bankAccountRepository.update({ !it.isActive() }) >> { BankAccount a -> a }
    }
}
