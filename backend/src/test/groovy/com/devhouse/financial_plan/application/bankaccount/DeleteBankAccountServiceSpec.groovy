package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import spock.lang.Specification

import java.time.Instant

class DeleteBankAccountServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    DeleteBankAccountService service = new DeleteBankAccountService(bankAccountRepository)

    def "execute deactivates the bank account instead of hard-deleting it"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        BankAccount account = new BankAccount(10L, 0, space, "Main Account", "BankCorp", new BigDecimal("500.00"), true, Instant.now(), null)
        bankAccountRepository.findById(10L) >> account

        when:
        service.execute(10L)

        then:
        1 * bankAccountRepository.update({ !it.isActive() })
    }
}
