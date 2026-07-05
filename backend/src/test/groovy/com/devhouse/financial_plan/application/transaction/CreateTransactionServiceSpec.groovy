package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateTransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    PaymentMethodRepository paymentMethodRepository = Mock()
    UserRepository userRepository = Mock()

    CreateTransactionService service = new CreateTransactionService(transactionRepository, bankAccountRepository,
            categoryRepository, subCategoryRepository, paymentMethodRepository, userRepository)

    private CreateTransactionRequest buildRequest(TransactionType type, Long bankAccountId, Long destinationBankAccountId,
                                                   Long categoryId, Long subCategoryId, Long paymentMethodId) {
        new CreateTransactionRequest(type, 1L, bankAccountId, destinationBankAccountId, categoryId, subCategoryId,
                paymentMethodId, new BigDecimal("100.00"), LocalDate.now(), "desc")
    }

    private void stubExistingForeignKeys() {
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        bankAccountRepository.findById(2L) >> Mock(BankAccount)
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        subCategoryRepository.findById(30L) >> Mock(SubCategory)
    }

    def "execute creates INCOME transaction when all FKs exist"() {
        given:
        stubExistingForeignKeys()
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null, 20L)
        Transaction saved = new Transaction(5L, 0, TransactionType.INCOME, 1L, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 5L
        response.type() == TransactionType.INCOME
    }

    def "execute creates EXPENSE transaction when all FKs exist"() {
        given:
        stubExistingForeignKeys()
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null, 20L)
        Transaction saved = new Transaction(6L, 0, TransactionType.EXPENSE, 1L, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 6L
        response.type() == TransactionType.EXPENSE
    }

    def "execute creates TRANSFER transaction when both bank accounts exist"() {
        given:
        stubExistingForeignKeys()
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null, null)
        Transaction saved = new Transaction(7L, 0, TransactionType.TRANSFER, 1L, 1L, 2L, null, null, null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 7L
        response.type() == TransactionType.TRANSFER
        response.destinationBankAccountId() == 2L
    }

    def "execute throws DomainException when user does not exist"() {
        given:
        userRepository.findById(1L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null, 20L)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException when bank account does not exist"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null, 20L)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        categoryRepository.findById(10L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null, 20L)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException when payment method does not exist"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null, 20L)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException when sub category does not exist"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        subCategoryRepository.findById(30L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, 30L, 20L)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException when destination bank account does not exist"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        bankAccountRepository.findById(2L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null, null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute throws DomainException for TRANSFER when destination equals origin bank account"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount)
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 1L, null, null, null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }
}
