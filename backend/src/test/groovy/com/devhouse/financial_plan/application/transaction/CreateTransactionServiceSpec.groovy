package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
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
    TransactionBalanceEffectService balanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    CreateTransactionService service = new CreateTransactionService(transactionRepository, bankAccountRepository,
            categoryRepository, subCategoryRepository, paymentMethodRepository, userRepository, balanceEffectService)

    private CreateTransactionRequest buildRequest(TransactionType type, Long bankAccountId, Long destinationBankAccountId,
                                                   Long categoryId, Long subCategoryId, Long paymentMethodId) {
        new CreateTransactionRequest(type, 1L, bankAccountId, destinationBankAccountId, categoryId, subCategoryId,
                paymentMethodId, new BigDecimal("100.00"), LocalDate.now(), "desc")
    }

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

    def "execute creates INCOME transaction and credits the bank account"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null, 20L)
        Transaction saved = new Transaction(5L, 0, TransactionType.INCOME, buildUser(1L), account, null,
                buildCategoryObj(10L), null, buildPaymentMethodObj(20L),
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 5L
        response.type() == TransactionType.INCOME
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("600.00") })
    }

    def "execute creates EXPENSE transaction and debits the bank account"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category)
        paymentMethodRepository.findById(20L) >> Mock(PaymentMethod)
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null, 20L)
        Transaction saved = new Transaction(6L, 0, TransactionType.EXPENSE, buildUser(1L), account, null,
                buildCategoryObj(10L), null, buildPaymentMethodObj(20L),
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 6L
        response.type() == TransactionType.EXPENSE
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("400.00") })
    }

    def "execute creates TRANSFER transaction, debiting origin and crediting destination"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        BankAccount origin = buildAccount(1L, new BigDecimal("500.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("200.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null, null)
        Transaction saved = new Transaction(7L, 0, TransactionType.TRANSFER, buildUser(1L), origin, destination,
                null, null, null, new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(request)

        then:
        response.id() == 7L
        response.type() == TransactionType.TRANSFER
        response.destinationBankAccountId() == 2L
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("400.00") })
        1 * bankAccountRepository.update({ it.id == 2L && it.balance == new BigDecimal("300.00") })
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
        0 * bankAccountRepository.update(_)
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
        0 * bankAccountRepository.update(_)
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
        0 * bankAccountRepository.update(_)
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
        0 * bankAccountRepository.update(_)
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
        0 * bankAccountRepository.update(_)
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
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException for TRANSFER when destination equals origin bank account"() {
        given:
        userRepository.findById(1L) >> Mock(User)
        bankAccountRepository.findById(1L) >> buildAccount(1L, BigDecimal.ZERO)
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 1L, null, null, null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }
}
