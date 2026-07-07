package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class TransactionBalanceEffectServiceSpec extends Specification {

    BankAccountRepository bankAccountRepository = Mock()
    TransactionBalanceEffectService service = new TransactionBalanceEffectService(bankAccountRepository)

    private BankAccount buildAccount(Long id, BigDecimal balance) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(id, 0, space, "Account " + id, "BankCorp", balance, true, Instant.now(), null)
    }

    private User buildUser(Long id) {
        new User(id, 0, "auth0|" + id, "User " + id, null, null, null, null, "user${id}@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category buildCategoryObj(Long id) {
        id == null ? null : new Category(id, 0, null, "Category " + id, true, Instant.now(), null)
    }

    private PaymentMethod buildPaymentMethodObj(Long id) {
        id == null ? null : new PaymentMethod(id, 0, null, "Method " + id, true, Instant.now(), null)
    }

    private Transaction buildTransaction(TransactionType type, Long bankAccountId, Long destinationBankAccountId) {
        Long categoryId = TransactionType.TRANSFER.equals(type) ? null : 10L
        Long paymentMethodId = TransactionType.TRANSFER.equals(type) ? null : 20L
        new Transaction(1L, 0, type, buildUser(1L), buildAccount(bankAccountId, BigDecimal.ZERO),
                destinationBankAccountId != null ? buildAccount(destinationBankAccountId, BigDecimal.ZERO) : null,
                buildCategoryObj(categoryId), null, buildPaymentMethodObj(paymentMethodId),
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
    }

    def "apply credits the bank account for INCOME"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.INCOME, 1L, null)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))

        when:
        service.apply(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> account
        1 * bankAccountRepository.update({ it.balance == new BigDecimal("600.00") })
    }

    def "apply debits the bank account for EXPENSE"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.EXPENSE, 1L, null)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))

        when:
        service.apply(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> account
        1 * bankAccountRepository.update({ it.balance == new BigDecimal("400.00") })
    }

    def "apply debits origin and credits destination for TRANSFER"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.TRANSFER, 1L, 2L)
        BankAccount origin = buildAccount(1L, new BigDecimal("500.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("200.00"))

        when:
        service.apply(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> origin
        1 * bankAccountRepository.findById(2L) >> destination
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("400.00") })
        1 * bankAccountRepository.update({ it.id == 2L && it.balance == new BigDecimal("300.00") })
    }

    def "revert debits the bank account for INCOME"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.INCOME, 1L, null)
        BankAccount account = buildAccount(1L, new BigDecimal("600.00"))

        when:
        service.revert(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> account
        1 * bankAccountRepository.update({ it.balance == new BigDecimal("500.00") })
    }

    def "revert credits the bank account for EXPENSE"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.EXPENSE, 1L, null)
        BankAccount account = buildAccount(1L, new BigDecimal("400.00"))

        when:
        service.revert(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> account
        1 * bankAccountRepository.update({ it.balance == new BigDecimal("500.00") })
    }

    def "revert credits origin and debits destination for TRANSFER"() {
        given:
        Transaction transaction = buildTransaction(TransactionType.TRANSFER, 1L, 2L)
        BankAccount origin = buildAccount(1L, new BigDecimal("400.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("300.00"))

        when:
        service.revert(transaction)

        then:
        1 * bankAccountRepository.findById(1L) >> origin
        1 * bankAccountRepository.findById(2L) >> destination
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("500.00") })
        1 * bankAccountRepository.update({ it.id == 2L && it.balance == new BigDecimal("200.00") })
    }
}
