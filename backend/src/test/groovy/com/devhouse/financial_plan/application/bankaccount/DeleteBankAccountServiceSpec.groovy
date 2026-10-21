package com.devhouse.financial_plan.application.bankaccount

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant

class DeleteBankAccountServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteBankAccountService service = new DeleteBankAccountService(bankAccountRepository, transactionRepository)

    def setup() {
        bankAccountRepository.findById(10L) >> buildAccount(10L, 1L)
    }

    private BankAccount buildAccount(Long id, Long spaceId) {
        Space space = new Space(spaceId, 0, "Space " + spaceId, null, Instant.now(), null)
        new BankAccount(id, 0, space, "Account", "BankCorp", BigDecimal.ZERO, true, Instant.now(), null)
    }

    def "execute hard-deletes the bank account when there are no linked transactions"() {
        given:
        transactionRepository.existsByBankAccountId(10L) >> false

        when:
        service.execute(1L, 10L)

        then:
        1 * bankAccountRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are linked transactions"() {
        given:
        transactionRepository.existsByBankAccountId(10L) >> true

        when:
        service.execute(1L, 10L)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.delete(_)
    }

    def "execute throws DomainException and does not delete when the bank account belongs to another space"() {
        when:
        service.execute(2L, 10L)

        then:
        DomainException exception = thrown()
        exception.message == "Bank account not found"
        0 * bankAccountRepository.delete(_)
    }

    def "execute throws DomainException when the bank account does not exist"() {
        when:
        service.execute(1L, 99L)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.delete(_)
    }
}
