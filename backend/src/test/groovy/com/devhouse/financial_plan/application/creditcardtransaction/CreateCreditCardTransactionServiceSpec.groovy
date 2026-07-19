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
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        CreditCardTransactionResponse response = service.execute(request)

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

    def "execute creates a single credit transaction and forces one installment even when installments are requested"() {
        given:
        CreditCard creditCard = buildCreditCard()
        creditCardRepository.findById(10L) >> creditCard
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), true, LocalDate.of(2026, 3, 5), "Cashback", 6)

        when:
        CreditCardTransactionResponse response = service.execute(request)

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
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", 3)

        when:
        service.execute(request)

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
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 7, 12), "desc", 2)

        when:
        service.execute(request)

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
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(_, _) >> null
        List<CreditCardTransaction> saved = []
        creditCardTransactionRepository.save(_) >> { CreditCardTransaction t -> saved << t; t }

        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", 3)

        when:
        service.execute(request)

        then:
        saved[0].amount == new BigDecimal("33.33")
        saved[1].amount == new BigDecimal("33.33")
        saved[2].amount == new BigDecimal("33.34")
        saved.collect { it.amount }.sum() == new BigDecimal("100.00")
    }

    def "execute throws DomainException when credit card does not exist"() {
        given:
        creditCardRepository.findById(99L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(99L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when user does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findById(1L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when sub category does not exist"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        subCategoryRepository.findById(30L) >> null
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, 30L,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }

    def "execute throws DomainException when the target invoice month is already paid"() {
        given:
        creditCardRepository.findById(10L) >> buildCreditCard()
        userRepository.findById(1L) >> Mock(User)
        categoryRepository.findById(20L) >> Mock(Category)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> Mock(CreditCardInvoicePayment)
        CreateCreditCardTransactionRequest request = new CreateCreditCardTransactionRequest(10L, 1L, 20L, null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 3, 5), "desc", null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.save(_)
    }
}
