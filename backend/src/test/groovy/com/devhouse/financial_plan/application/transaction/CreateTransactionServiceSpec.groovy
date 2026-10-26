package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.domain.exception.NotFoundException
import com.devhouse.financial_plan.application.category.ResolveSystemCategoryService
import com.devhouse.financial_plan.domain.enums.CategoryKind
import com.devhouse.financial_plan.application.category.dto.SystemCategoryPair
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.SystemCategory
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
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
    UserRepository userRepository = Mock()
    TransactionBalanceEffectService balanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    ResolveSystemCategoryService resolveSystemCategoryService = Mock()

    CreateTransactionService service = new CreateTransactionService(transactionRepository, bankAccountRepository,
            categoryRepository, subCategoryRepository, userRepository, balanceEffectService, resolveSystemCategoryService)

    def setup() {
        resolveSystemCategoryService.execute(_, SystemCategory.TRANSFER) >> new SystemCategoryPair(90L, null)
        categoryRepository.findById(90L) >> new Category(90L, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Transferência", CategoryKind.EXPENSE, true, Instant.now(), null)
    }

    private CreateTransactionRequest buildRequest(TransactionType type, Long bankAccountId, Long destinationBankAccountId,
                                                   Long categoryId, Long subCategoryId) {
        new CreateTransactionRequest(type, bankAccountId, destinationBankAccountId, categoryId, subCategoryId,
                new BigDecimal("100.00"), LocalDate.now(), "desc")
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
        id == null ? null : new Category(id, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Category " + id, CategoryKind.EXPENSE, true, Instant.now(), null)
    }

    def "execute creates INCOME transaction and credits the bank account"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category) { belongsToSpace(_) >> true }
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null)
        Transaction saved = new Transaction(5L, 0, TransactionType.INCOME, buildUser(1L), account, null,
                buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null, null, null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(1L, "auth0|1", request)

        then:
        response.id() == 5L
        response.type() == TransactionType.INCOME
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("600.00") })
    }

    def "execute creates EXPENSE transaction and debits the bank account"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        BankAccount account = buildAccount(1L, new BigDecimal("500.00"))
        bankAccountRepository.findById(1L) >> account
        categoryRepository.findById(10L) >> Mock(Category) { belongsToSpace(_) >> true }
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null)
        Transaction saved = new Transaction(6L, 0, TransactionType.EXPENSE, buildUser(1L), account, null,
                buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null, null, null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(1L, "auth0|1", request)

        then:
        response.id() == 6L
        response.type() == TransactionType.EXPENSE
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("400.00") })
    }

    def "execute creates TRANSFER transaction, debiting origin and crediting destination"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        BankAccount origin = buildAccount(1L, new BigDecimal("500.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("200.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null)
        Transaction saved = new Transaction(7L, 0, TransactionType.TRANSFER, buildUser(1L), origin, destination,
                null, null, new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null, null, null)
        transactionRepository.save(_) >> saved

        when:
        TransactionResponse response = service.execute(1L, "auth0|1", request)

        then:
        response.id() == 7L
        response.type() == TransactionType.TRANSFER
        response.destinationBankAccountId() == 2L
        1 * bankAccountRepository.update({ it.id == 1L && it.balance == new BigDecimal("400.00") })
        1 * bankAccountRepository.update({ it.id == 2L && it.balance == new BigDecimal("300.00") })
    }

    def "execute throws DomainException when user does not exist"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException when bank account does not exist"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount) { belongsToSpace(_) >> true }
        categoryRepository.findById(10L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException when sub category does not exist"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount) { belongsToSpace(_) >> true }
        categoryRepository.findById(10L) >> Mock(Category) { belongsToSpace(_) >> true }
        subCategoryRepository.findById(30L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.INCOME, 1L, null, 10L, 30L)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException when destination bank account does not exist"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> Mock(BankAccount) { belongsToSpace(_) >> true }
        bankAccountRepository.findById(2L) >> null
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute throws DomainException for TRANSFER when destination equals origin bank account"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> buildAccount(1L, BigDecimal.ZERO)
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 1L, null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }


    def "execute stamps the TRANSFER system category on a transfer instead of leaving it uncategorised"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        BankAccount origin = buildAccount(1L, new BigDecimal("500.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("200.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination
        CreateTransactionRequest request = buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null)
        Transaction captured = null

        when:
        service.execute(1L, "auth0|1", request)

        then:
        1 * transactionRepository.save(_) >> { Transaction t ->
            captured = t
            t
        }
        captured.getCategory().getId() == 90L
        captured.getCategory().getName() == "Transferência"
        captured.getCategory().isSystem()
        captured.getSubCategory() == null
    }

    def "execute throws DomainException when a system category is selected on a user owned transaction"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        categoryRepository.findById(10L) >> new Category(10L, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Pagamento de Fatura", CategoryKind.EXPENSE, true, Instant.now(), null)
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * transactionRepository.save(_)
    }

    def "execute accepts the system category when the transaction is generated by the system"() {
        given:
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        categoryRepository.findById(10L) >> new Category(10L, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Pagamento de Fatura", CategoryKind.EXPENSE, true, Instant.now(), null)
        CreateTransactionRequest request = buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null)
        Transaction captured = null

        when:
        service.execute(1L, Mock(User), request, TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 10L)

        then:
        1 * transactionRepository.save(_) >> { Transaction t ->
            captured = t
            t
        }
        captured.getCategory().getId() == 10L
        captured.getSourceType() == TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT
    }

    def "execute records the transaction for the authenticated user resolved from auth0Sub"() {
        given:
        User authenticated = buildUser(7L)
        userRepository.findByAuth0Sub("auth0|1") >> authenticated
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        categoryRepository.findById(10L) >> buildCategoryObj(10L)
        Transaction captured = null

        when:
        service.execute(1L, "auth0|1", buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null))

        then:
        1 * transactionRepository.save(_) >> { Transaction t ->
            captured = t
            t
        }
        captured.getUser().getId() == 7L
    }

    def "execute rejects a bank account that belongs to another space"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> buildUser(1L)
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        categoryRepository.findById(10L) >> buildCategoryObj(10L)

        when:
        service.execute(2L, "auth0|1", buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null))

        then:
        DomainException exception = thrown()
        exception.message == "Bank account not found"
        !(exception instanceof NotFoundException)
        0 * transactionRepository.save(_)
        0 * bankAccountRepository.update(_)
    }

    def "execute rejects a destination bank account that belongs to another space"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> buildUser(1L)
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        Space otherSpace = new Space(2L, 0, "Other Space", null, Instant.now(), null)
        bankAccountRepository.findById(2L) >> new BankAccount(2L, 0, otherSpace, "Foreign", "BankCorp", BigDecimal.ZERO, true,
                Instant.now(), null)

        when:
        service.execute(1L, "auth0|1", buildRequest(TransactionType.TRANSFER, 1L, 2L, null, null))

        then:
        DomainException exception = thrown()
        exception.message == "Destination bank account not found"
        0 * transactionRepository.save(_)
    }

    def "execute rejects a category that belongs to another space"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> buildUser(1L)
        bankAccountRepository.findById(1L) >> buildAccount(1L, new BigDecimal("500.00"))
        Space otherSpace = new Space(2L, 0, "Other Space", null, Instant.now(), null)
        categoryRepository.findById(10L) >> new Category(10L, 0, otherSpace, "Foreign", CategoryKind.EXPENSE, true, Instant.now(), null)

        when:
        service.execute(1L, "auth0|1", buildRequest(TransactionType.EXPENSE, 1L, null, 10L, null))

        then:
        DomainException exception = thrown()
        exception.message == "Category not found"
        0 * transactionRepository.save(_)
    }
}
