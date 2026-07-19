package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.application.report.dto.CategoryReportFilterRequest
import com.devhouse.financial_plan.application.report.dto.CategoryReportItemSource
import com.devhouse.financial_plan.application.report.dto.CategoryReportResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateCategoryReportServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    GenerateCategoryReportService service = new GenerateCategoryReportService(transactionRepository, creditCardTransactionRepository)

    Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
    User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
            null, null, Instant.now(), null, false)
    BankAccount bankAccount = new BankAccount(5L, 0, space, "Conta Corrente", "Nubank", BigDecimal.ZERO, true, Instant.now(), null)
    Category food = new Category(20L, 0, space, "Alimentação", true, Instant.now(), null)
    Category salary = new Category(21L, 0, space, "Salário", true, Instant.now(), null)
    SubCategory restaurants = new SubCategory(30L, 0, food, "Restaurantes", true, Instant.now(), null)

    private CategoryReportFilterRequest filter(Map overrides = [:]) {
        Map values = [spaceId: 1L, from: LocalDate.of(2026, 7, 1), to: LocalDate.of(2026, 7, 31), userId: null,
                      bankAccountId: null, categoryId: null, subCategoryId: null,
                      type: null, creditCardId: null] + overrides
        new CategoryReportFilterRequest(values.spaceId, values.from, values.to, values.userId, values.bankAccountId,
                values.categoryId, values.subCategoryId, values.type, values.creditCardId)
    }

    private Transaction transaction(Map overrides = [:]) {
        Map values = [id: 100L, type: TransactionType.EXPENSE, category: food, subCategory: null,
                      amount: new BigDecimal("50.00"), date: LocalDate.of(2026, 7, 10), sourceType: null] + overrides
        new Transaction(values.id, 0, values.type, user, bankAccount, null, values.category, values.subCategory,
                values.amount, values.date, "desc", Instant.now(), null, values.sourceType, null)
    }

    private CreditCard creditCard(Map overrides = [:]) {
        Map values = [id: 10L, name: "Nubank", bankAccount: null] + overrides
        new CreditCard(values.id, 0, space, values.bankAccount, values.name, new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    // installmentNumber is fixed at 1, so the purchase's competenceMonth (derived) always equals the month of `date`.
    private CreditCardTransaction purchase(Map overrides = [:]) {
        Map values = [id: 200L, creditCard: creditCard(), category: food, subCategory: restaurants,
                      amount: new BigDecimal("80.00"), date: LocalDate.of(2026, 7, 15),
                      referenceMonth: LocalDate.of(2026, 8, 1)] + overrides
        new CreditCardTransaction(values.id, 0, values.creditCard, null, user, values.category, values.subCategory,
                values.amount, false, values.date, "purchase", values.referenceMonth,
                "group-1", 1, 3, false, null, Instant.now(), null)
    }

    def "execute throws DomainException when spaceId is missing"() {
        when:
        service.execute(filter(spaceId: null))

        then:
        thrown(DomainException)
        0 * transactionRepository.findByFilter(*_)
        0 * creditCardTransactionRepository.findByFilter(*_)
    }

    def "execute groups items by category and subcategory with a null-subcategory bucket"() {
        given:
        transactionRepository.findByFilter(1L, null, null, null, null, null, _, _) >>
                [transaction(id: 100L, amount: new BigDecimal("50.00")),
                 transaction(id: 101L, amount: new BigDecimal("30.00"), subCategory: restaurants)]
        creditCardTransactionRepository.findByFilter(1L, null, null, null, null, null) >>
                [purchase(id: 200L, amount: new BigDecimal("80.00"))]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.groups().size() == 1
        response.groups()[0].categoryId() == 20L
        response.groups()[0].categoryName() == "Alimentação"
        response.groups()[0].totalExpense() == new BigDecimal("160.00")
        response.groups()[0].subGroups().size() == 2
        response.groups()[0].subGroups()[0].subCategoryId() == 30L
        response.groups()[0].subGroups()[0].totalExpense() == new BigDecimal("110.00")
        response.groups()[0].subGroups()[1].subCategoryId() == null
        response.groups()[0].subGroups()[1].totalExpense() == new BigDecimal("50.00")
    }

    def "execute excludes credit card invoice payment transactions and transfers"() {
        given:
        transactionRepository.findByFilter(1L, null, null, null, null, null, _, _) >>
                [transaction(id: 100L),
                 transaction(id: 101L, sourceType: TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, amount: new BigDecimal("999.00")),
                 transaction(id: 102L, type: TransactionType.TRANSFER, category: null, amount: new BigDecimal("500.00"))]
        creditCardTransactionRepository.findByFilter(*_) >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.totalExpense() == new BigDecimal("50.00")
        response.groups().size() == 1
        response.groups()[0].subGroups()[0].items().size() == 1
        response.groups()[0].subGroups()[0].items()[0].id() == 100L
    }

    def "execute includes credit card purchases whose competence month falls in the requested period, showing the purchase date and invoice month"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        creditCardTransactionRepository.findByFilter(1L, null, null, null, null, null) >>
                [purchase(id: 200L)]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.groups()[0].subGroups()[0].items().size() == 1
        with(response.groups()[0].subGroups()[0].items()[0]) {
            source() == CategoryReportItemSource.CREDIT_CARD
            type() == TransactionType.EXPENSE
            creditCardId() == 10L
            creditCardName() == "Nubank"
            installmentNumber() == 1
            totalInstallments() == 3
            date() == LocalDate.of(2026, 7, 15)
            referenceMonth() == LocalDate.of(2026, 8, 1)
            dueDate() == LocalDate.of(2026, 8, 17)
        }
    }

    def "execute filters credit card purchases by the requested from/to range using the derived competence month"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        creditCardTransactionRepository.findByFilter(1L, null, null, null, null, null) >>
                [purchase(id: 200L, date: LocalDate.of(2026, 8, 20), referenceMonth: LocalDate.of(2026, 9, 1)),
                 purchase(id: 201L, date: LocalDate.of(2026, 9, 5), referenceMonth: LocalDate.of(2026, 10, 1))]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter(from: LocalDate.of(2026, 8, 1), to: LocalDate.of(2026, 8, 31)))

        then:
        response.groups()[0].subGroups()[0].items().size() == 1
        response.groups()[0].subGroups()[0].items()[0].id() == 200L
    }

    def "execute leaves referenceMonth and dueDate null for regular transaction items"() {
        given:
        transactionRepository.findByFilter(*_) >> [transaction(id: 100L)]
        creditCardTransactionRepository.findByFilter(*_) >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.groups()[0].subGroups()[0].items()[0].referenceMonth() == null
        response.groups()[0].subGroups()[0].items()[0].dueDate() == null
    }

    def "execute computes totalAmount by summing the installment group, once per group"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        creditCardTransactionRepository.findByFilter(*_) >>
                [purchase(id: 200L, amount: new BigDecimal("33.33")),
                 purchase(id: 201L, amount: new BigDecimal("33.33"), category: food, subCategory: restaurants)]

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        1 * creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> [
                purchase(id: 200L, amount: new BigDecimal("33.33")),
                purchase(id: 201L, amount: new BigDecimal("33.33")),
                purchase(id: 202L, amount: new BigDecimal("33.34"))]
        response.groups()[0].subGroups()[0].items().size() == 2
        response.groups()[0].subGroups()[0].items()[0].totalAmount() == new BigDecimal("100.00")
        response.groups()[0].subGroups()[0].items()[1].totalAmount() == new BigDecimal("100.00")
    }

    def "execute returns only purchases of the given credit card when creditCardId is filtered"() {
        given:
        creditCardTransactionRepository.findByFilter(1L, 10L, null, null, null, null) >> [purchase()]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter(creditCardId: 10L))

        then:
        0 * transactionRepository.findByFilter(*_)
        response.groups()[0].subGroups()[0].items()[0].creditCardId() == 10L
    }

    def "execute omits credit card items when filtering by income"() {
        given:
        transactionRepository.findByFilter(1L, null, null, null, null, TransactionType.INCOME, _, _) >>
                [transaction(id: 100L, type: TransactionType.INCOME, category: salary, amount: new BigDecimal("1000.00"))]

        when:
        CategoryReportResponse response = service.execute(filter(type: TransactionType.INCOME))

        then:
        0 * creditCardTransactionRepository.findByFilter(*_)
        response.totalIncome() == new BigDecimal("1000.00")
        response.totalExpense() == BigDecimal.ZERO
    }

    def "execute filters credit card items by the bank account linked to the card"() {
        given:
        CreditCard linkedCard = creditCard(id: 10L, bankAccount: bankAccount)
        CreditCard unlinkedCard = creditCard(id: 11L, name: "Itaú")
        transactionRepository.findByFilter(*_) >> []
        creditCardTransactionRepository.findByFilter(*_) >>
                [purchase(id: 200L, creditCard: linkedCard), purchase(id: 201L, creditCard: unlinkedCard)]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter(bankAccountId: 5L))

        then:
        response.groups()[0].subGroups()[0].items().size() == 1
        response.groups()[0].subGroups()[0].items()[0].id() == 200L
        response.groups()[0].subGroups()[0].items()[0].bankAccountId() == 5L
    }

    def "execute forwards the userId filter to the credit card repository"() {
        given:
        transactionRepository.findByFilter(1L, 1L, null, null, null, null, _, _) >> []

        when:
        service.execute(filter(userId: 1L))

        then:
        1 * creditCardTransactionRepository.findByFilter(1L, null, null, null, 1L, null) >> []
    }

    def "execute computes summary totals and group percentages"() {
        given:
        transactionRepository.findByFilter(*_) >>
                [transaction(id: 100L, type: TransactionType.INCOME, category: salary, amount: new BigDecimal("1000.00")),
                 transaction(id: 101L, category: food, amount: new BigDecimal("150.00"))]
        creditCardTransactionRepository.findByFilter(*_) >> [purchase(id: 200L, amount: new BigDecimal("50.00"))]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.totalIncome() == new BigDecimal("1000.00")
        response.totalExpense() == new BigDecimal("200.00")
        response.balance() == new BigDecimal("800.00")
        response.groups().size() == 2
        response.groups()[0].categoryName() == "Alimentação"
        response.groups()[0].expensePercentage() == new BigDecimal("100.00")
        response.groups()[0].incomePercentage() == new BigDecimal("0.00")
        response.groups()[0].total() == new BigDecimal("-200.00")
        response.groups()[1].categoryName() == "Salário"
        response.groups()[1].incomePercentage() == new BigDecimal("100.00")
        response.groups()[1].expensePercentage() == new BigDecimal("0.00")
    }

    def "execute returns zero percentages when the report totals are zero"() {
        given:
        transactionRepository.findByFilter(*_) >> [transaction()]
        creditCardTransactionRepository.findByFilter(*_) >> []

        when:
        CategoryReportResponse response = service.execute(filter())

        then:
        response.totalIncome() == BigDecimal.ZERO
        response.groups()[0].incomePercentage() == new BigDecimal("0.00")
        response.groups()[0].expensePercentage() == new BigDecimal("100.00")
    }
}
