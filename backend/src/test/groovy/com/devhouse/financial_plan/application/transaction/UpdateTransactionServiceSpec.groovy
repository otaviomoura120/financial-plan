package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UpdateTransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    PaymentMethodRepository paymentMethodRepository = Mock()
    TransactionBalanceEffectService balanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    UpdateTransactionService service = new UpdateTransactionService(transactionRepository, bankAccountRepository,
            categoryRepository, subCategoryRepository, paymentMethodRepository, balanceEffectService)

    private BankAccount buildAccount(Long id, BigDecimal balance) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(id, 0, space, "Account " + id, "BankCorp", balance, true, Instant.now(), null)
    }

    private Transaction buildExistingTransaction(TransactionType type, Long bankAccountId, Long destinationBankAccountId,
                                                  BigDecimal amount) {
        Long categoryId = TransactionType.TRANSFER.equals(type) ? null : 10L
        Long paymentMethodId = TransactionType.TRANSFER.equals(type) ? null : 20L
        new Transaction(1L, 0, type, 1L, bankAccountId, destinationBankAccountId, categoryId, null,
                paymentMethodId, amount, LocalDate.now(), "desc", Instant.now(), null)
    }

    def "execute reverts old amount and applies new amount on the same EXPENSE account"() {
        given:
        Transaction existing = buildExistingTransaction(TransactionType.EXPENSE, 1L, null, new BigDecimal("100.00"))
        transactionRepository.findById(1L) >> existing
        transactionRepository.update(_) >> { Transaction t -> t }
        BankAccount account = buildAccount(1L, new BigDecimal("400.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        UpdateTransactionRequest request = new UpdateTransactionRequest(0, TransactionType.EXPENSE, 1L, null, 10L, null, 20L,
                new BigDecimal("150.00"), LocalDate.now(), "desc")

        when:
        TransactionResponse response = service.execute(1L, request)

        then:
        response.amount() == new BigDecimal("150.00")
        2 * bankAccountRepository.update(_)
        account.getBalance() == new BigDecimal("350.00")
    }

    def "execute reverts old INCOME and applies new EXPENSE on the same account"() {
        given:
        Transaction existing = buildExistingTransaction(TransactionType.INCOME, 1L, null, new BigDecimal("100.00"))
        transactionRepository.findById(1L) >> existing
        transactionRepository.update(_) >> { Transaction t -> t }
        BankAccount account = buildAccount(1L, new BigDecimal("600.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        UpdateTransactionRequest request = new UpdateTransactionRequest(0, TransactionType.EXPENSE, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc")

        when:
        TransactionResponse response = service.execute(1L, request)

        then:
        response.type() == TransactionType.EXPENSE
        2 * bankAccountRepository.update(_)
        account.getBalance() == new BigDecimal("400.00")
    }

    def "execute moves the EXPENSE effect from the old bank account to the new one"() {
        given:
        Transaction existing = buildExistingTransaction(TransactionType.EXPENSE, 1L, null, new BigDecimal("100.00"))
        transactionRepository.findById(1L) >> existing
        transactionRepository.update(_) >> { Transaction t -> t }
        BankAccount origin = buildAccount(1L, new BigDecimal("400.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("200.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        UpdateTransactionRequest request = new UpdateTransactionRequest(0, TransactionType.EXPENSE, 2L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc")

        when:
        service.execute(1L, request)

        then:
        origin.getBalance() == new BigDecimal("500.00")
        destination.getBalance() == new BigDecimal("100.00")
    }

    def "execute moves the TRANSFER destination from the old account to the new one"() {
        given:
        Transaction existing = buildExistingTransaction(TransactionType.TRANSFER, 1L, 2L, new BigDecimal("100.00"))
        transactionRepository.findById(1L) >> existing
        transactionRepository.update(_) >> { Transaction t -> t }
        BankAccount origin = buildAccount(1L, new BigDecimal("400.00"))
        BankAccount oldDestination = buildAccount(2L, new BigDecimal("300.00"))
        BankAccount newDestination = buildAccount(3L, new BigDecimal("50.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> oldDestination
        bankAccountRepository.findById(3L) >> newDestination
        UpdateTransactionRequest request = new UpdateTransactionRequest(0, TransactionType.TRANSFER, 1L, 3L, null, null, null,
                new BigDecimal("100.00"), LocalDate.now(), "desc")

        when:
        service.execute(1L, request)

        then:
        origin.getBalance() == new BigDecimal("400.00")
        oldDestination.getBalance() == new BigDecimal("200.00")
        newDestination.getBalance() == new BigDecimal("150.00")
    }

    def "execute throws DomainException when new bank account does not exist"() {
        given:
        Transaction existing = buildExistingTransaction(TransactionType.EXPENSE, 1L, null, new BigDecimal("100.00"))
        transactionRepository.findById(1L) >> existing
        bankAccountRepository.findById(1L) >> null
        UpdateTransactionRequest request = new UpdateTransactionRequest(0, TransactionType.EXPENSE, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc")

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.update(_)
        0 * transactionRepository.update(_)
    }
}
