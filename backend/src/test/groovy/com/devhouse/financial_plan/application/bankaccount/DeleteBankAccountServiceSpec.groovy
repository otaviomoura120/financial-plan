package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

class DeleteBankAccountServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteBankAccountService service = new DeleteBankAccountService(bankAccountRepository, transactionRepository)

    def "execute hard-deletes the bank account when there are no linked transactions"() {
        given:
        transactionRepository.existsByBankAccountId(10L) >> false

        when:
        service.execute(10L)

        then:
        1 * bankAccountRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are linked transactions"() {
        given:
        transactionRepository.existsByBankAccountId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.delete(_)
    }
}
