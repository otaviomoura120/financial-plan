package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRequest
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import com.devhouse.financial_plan.domain.enums.CategoryKind
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateCreditCardTransactionServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardRepository creditCardRepository = Mock()
    UserRepository userRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    CreateCreditCardTransactionService service = new CreateCreditCardTransactionService(creditCardTransactionRepository,
            creditCardRepository, userRepository, categoryRepository, subCategoryRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    def "execute creates a single cash-purchase transaction with installmentNumber=1 and totalInstallments=1"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        CreditCardTransactionResponse response = service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 1
        saved[0].installmentNumber == 1
        saved[0].totalInstallments == 1
        saved[0].referenceMonth == LocalDate.of(2026, 3, 1)
        saved[0].competenceMonth == LocalDate.of(2026, 3, 1)
        saved[0].amount == new BigDecimal("100.00")
        !saved[0].installmentGroupId.isBlank()
        response.installmentNumber() == 1
        response.totalInstallments() == 1
        response.totalAmount() == new BigDecimal("100.00")
    }

    def "execute honours an explicit choice of the next invoice, overriding the computed one"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, LocalDate.of(2026, 4, 1))

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 1
        saved[0].referenceMonth == LocalDate.of(2026, 4, 1)
    }

    def "execute anchors every installment on the explicitly chosen invoice"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("300.00"), false, LocalDate.of(2026, 3, 5), "desc", 3, LocalDate.of(2026, 4, 1))

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 3
        saved*.referenceMonth == [LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1)]
    }

    def "execute rejects an invoice that is neither the computed one nor the next"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, LocalDate.of(2026, 8, 1))

        when:
        service.execute(1L, "auth0|1", request)

        then:
        DomainException exception = thrown(DomainException)
        exception.message == "Reference month must be the current or the next invoice"
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute rejects a chosen invoice that is already paid"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 4, 1)) >> Mock(CreditCardInvoicePayment)

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, LocalDate.of(2026, 4, 1))

        when:
        service.execute(1L, "auth0|1", request)

        then:
        DomainException exception = thrown(DomainException)
        exception.message == "Cannot create a transaction in an invoice that is already paid"
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute creates a single credit transaction and forces one installment even when installments are requested"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), true, LocalDate.of(2026, 3, 5), "Cashback", 6, null)

        when:
        CreditCardTransactionResponse response = service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 1
        saved[0].credit
        saved[0].totalInstallments == 1
        saved[0].installmentNumber == 1
        saved[0].amount == new BigDecimal("100.00")
        saved[0].signedAmount == new BigDecimal("-100.00")
        response.credit()
        response.amount() == new BigDecimal("100.00")
    }

    def "execute splits an installment purchase across N rows sharing the same group id and sequential reference months"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", 3, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 3
        saved.collect { it.installmentGroupId }.unique().size() == 1
        saved[0].installmentNumber == 1
        saved[1].installmentNumber == 2
        saved[2].installmentNumber == 3
        saved.every { it.totalInstallments == 3 }
        saved[0].referenceMonth == LocalDate.of(2026, 3, 1)
        saved[1].referenceMonth == LocalDate.of(2026, 4, 1)
        saved[2].referenceMonth == LocalDate.of(2026, 5, 1)
        saved[0].competenceMonth == LocalDate.of(2026, 3, 1)
        saved[1].competenceMonth == LocalDate.of(2026, 4, 1)
        saved[2].competenceMonth == LocalDate.of(2026, 5, 1)
        saved.collect { it.amount }.sum() == new BigDecimal("100.00")
    }

    def "execute anchors competenceMonth to the purchase month even when the closing day pushes referenceMonth to the next month"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 1, 10, true, Instant.now(), null)
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 7, 12), "desc", 2, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved[0].referenceMonth == LocalDate.of(2026, 8, 1)
        saved[0].competenceMonth == LocalDate.of(2026, 7, 1)
        saved[1].referenceMonth == LocalDate.of(2026, 9, 1)
        saved[1].competenceMonth == LocalDate.of(2026, 8, 1)
    }

    def "execute makes the last installment absorb the rounding residue when the amount does not divide exactly"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", 3, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved[0].amount == new BigDecimal("33.33")
        saved[1].amount == new BigDecimal("33.33")
        saved[2].amount == new BigDecimal("33.34")
        saved.collect { it.amount }.sum() == new BigDecimal("100.00")
    }

    def "execute rejects an out-of-range installment count before touching any invoice or saving"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", installments, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        DomainException exception = thrown()
        exception.message == "Total installments must be between 1 and 60"
        0 * creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _)
        0 * creditCardTransactionRepository.save(_)

        where:
        installments << [61, Integer.MAX_VALUE]
    }

    def "execute accepts the maximum of 60 installments"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("600.00"), false, LocalDate.of(2026, 3, 5), "desc", 60, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        saved.size() == 60
        saved*.totalInstallments.every { it == 60 }
    }

    def "execute throws DomainException when credit card does not exist"() {
        given:
        creditCardRepository.findById(99L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(99L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when user does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when sub category does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        subCategoryRepository.findById(30L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, 30L,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when the target invoice month is already paid"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category) { belongsToSpace(_) >> true }
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute accepts an income category on an invoice credit and an expense category on a purchase"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> new Category(20L, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Category", kind, true, Instant.now(), null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), credit, LocalDate.of(2026, 3, 5), "Entry", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        1 * creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> t }

        where:
        credit | kind
        true   | CategoryKind.INCOME
        false  | CategoryKind.EXPENSE
    }

    def "execute rejects a category whose kind does not match the entry"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        categoryRepository.findById(20L) >> new Category(20L, 0, new Space(1L, 0, "Space", null, Instant.now(), null), "Category", kind, true, Instant.now(), null)
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), credit, LocalDate.of(2026, 3, 5), "Entry", null, null)

        when:
        service.execute(1L, "auth0|1", request)

        then:
        DomainException exception = thrown()
        exception.message == message
        0 * creditCardTransactionRepository.save(_)

        where:
        credit | kind                 || message
        true   | CategoryKind.EXPENSE || "The selected category is not an income category"
        false  | CategoryKind.INCOME  || "The selected category is not an expense category"
    }

    def "execute rejects a credit card that belongs to another space"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findByAuth0Sub("auth0|1") >> Mock(User)
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", 1, null)

        when:
        service.execute(2L, "auth0|1", request)

        then:
        DomainException exception = thrown()
        exception.message == "Credit card not found"
        0 * creditCardTransactionRepository.save(_)
    }
}
