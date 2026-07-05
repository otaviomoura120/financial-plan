package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeleteTransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    TransactionBalanceEffectService balanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    DeleteTransactionService service = new DeleteTransactionService(transactionRepository, balanceEffectService)

    private BankAccount buildAccount(Long id, BigDecimal balance) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(id, 0, space, "Account " + id, "BankCorp", balance, true, Instant.now(), null)
    }

    def "execute reverts the EXPENSE effect and deletes the transaction"() {
        given:
        Transaction transaction = new Transaction(1L, 0, TransactionType.EXPENSE, 1L, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.findById(1L) >> transaction
        BankAccount account = buildAccount(1L, new BigDecimal("400.00"))
        bankAccountRepository.findById(1L) >> account

        when:
        service.execute(1L)

        then:
        1 * bankAccountRepository.update(_)
        1 * transactionRepository.delete(1L)
        account.getBalance() == new BigDecimal("500.00")
    }

    def "execute reverts the TRANSFER effect on both accounts and deletes the transaction"() {
        given:
        Transaction transaction = new Transaction(2L, 0, TransactionType.TRANSFER, 1L, 1L, 2L, null, null, null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.findById(2L) >> transaction
        BankAccount origin = buildAccount(1L, new BigDecimal("400.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("300.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination

        when:
        service.execute(2L)

        then:
        1 * transactionRepository.delete(2L)
        origin.getBalance() == new BigDecimal("500.00")
        destination.getBalance() == new BigDecimal("200.00")
    }

    def "execute throws DomainException when transaction does not exist"() {
        given:
        transactionRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.update(_)
        0 * transactionRepository.delete(_)
    }
}
