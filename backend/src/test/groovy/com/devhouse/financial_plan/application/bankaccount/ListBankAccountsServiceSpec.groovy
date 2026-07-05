package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import spock.lang.Specification

import java.time.Instant

class ListBankAccountsServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    ListBankAccountsService service = new ListBankAccountsService(bankAccountRepository)

    def "execute returns the bank accounts of the space"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        BankAccount account = new BankAccount(10L, 0, space, "Main Account", "BankCorp", new BigDecimal("500.00"), true, Instant.now(), null)
        bankAccountRepository.findBySpaceId(1L) >> [account]

        when:
        List<BankAccountResponse> responses = service.execute(1L)

        then:
        responses.size() == 1
        responses[0].id() == 10L
        responses[0].spaceId() == 1L
        responses[0].name() == "Main Account"
    }

    def "execute returns an empty list when the space has no bank accounts"() {
        given:
        bankAccountRepository.findBySpaceId(99L) >> []

        when:
        List<BankAccountResponse> responses = service.execute(99L)

        then:
        responses.isEmpty()
    }
}
