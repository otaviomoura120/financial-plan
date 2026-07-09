package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreditCardInvoiceCategoryMatcherSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    TransactionRepository transactionRepository = Mock()

    CreditCardInvoiceCategoryMatcher matcher = new CreditCardInvoiceCategoryMatcher(
            creditCardTransactionRepository, creditCardInvoicePaymentRepository, transactionRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private User buildUser(Long id) {
        new User(id, 0, "auth0|" + id, "User " + id, null, null, null, null, "user${id}@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private BankAccount buildAccount(Long id) {
        new BankAccount(id, 0, buildSpace(), "Account " + id, "BankCorp", BigDecimal.ZERO, true, Instant.now(), null)
    }

    private CreditCard buildCreditCard(Long id) {
        new CreditCard(id, 0, buildSpace(), "Card " + id, new BigDecimal("1000.00"), 20, 27, true, Instant.now(), null)
    }

    private CreditCardTransaction buildCreditCardItem(Long creditCardId, LocalDate referenceMonth, Long categoryId, BigDecimal amount) {
        Category category = new Category(categoryId, 0, null, "Category " + categoryId, true, Instant.now(), null)
        new CreditCardTransaction(100L, 0, buildCreditCard(creditCardId), buildUser(1L), category, null, amount,
                referenceMonth, "item", referenceMonth, "group-1", 1, 1, false, null, Instant.now(), null)
    }

    private Transaction buildInvoicePaymentTransaction(Long id, Long creditCardId, BigDecimal amount, LocalDate transactionDate) {
        Category category = new Category(10L, 0, null, "Cartão de Crédito", true, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(20L, 0, null, "Cash", true, Instant.now(), null)
        new Transaction(id, 0, TransactionType.EXPENSE, buildUser(1L), buildAccount(1L), null, category, null,
                paymentMethod, amount, transactionDate, "Pagamento de fatura", Instant.now(), null,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, creditCardId)
    }

    private CreditCardInvoicePayment buildInvoicePayment(Long creditCardId, LocalDate referenceMonth, Long paymentTransactionId) {
        new CreditCardInvoicePayment(1L, 0, buildCreditCard(creditCardId), referenceMonth, referenceMonth.plusDays(10),
                new BigDecimal("450.00"), referenceMonth.plusDays(10), paymentTransactionId, 1L, Instant.now(), null)
    }

    def "resolveMatchingInvoiceAmounts returns noFilter and never queries the repository when no category filter is set"() {
        given:
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        MatchingInvoiceAmounts result = matcher.resolveMatchingInvoiceAmounts(filter)

        then:
        !result.hasCategoryFilter()
        result.isEmpty()
        0 * creditCardTransactionRepository.findByFilter(*_)
    }

    def "resolveMatchingInvoiceAmounts queries the repository without a purchaseDate window, regardless of the report's from/to"() {
        given:
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, 7L, 8L, null, null)
        creditCardTransactionRepository.findByFilter(*_) >> []

        when:
        matcher.resolveMatchingInvoiceAmounts(filter)

        then:
        1 * creditCardTransactionRepository.findByFilter(1L, null, 7L, 8L, null, null, null) >> []
    }

    def "resolveMatchingInvoiceAmounts sums items sharing the same credit card and reference month, keeping different months separate"() {
        given:
        LocalDate march = LocalDate.of(2026, 3, 1)
        LocalDate april = LocalDate.of(2026, 4, 1)
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, 7L, null, null, null)
        creditCardTransactionRepository.findByFilter(*_) >> [
                buildCreditCardItem(5L, march, 7L, new BigDecimal("120.00")),
                buildCreditCardItem(5L, march, 7L, new BigDecimal("30.00")),
                buildCreditCardItem(5L, april, 7L, new BigDecimal("50.00"))
        ]

        when:
        MatchingInvoiceAmounts result = matcher.resolveMatchingInvoiceAmounts(filter)

        then:
        result.hasCategoryFilter()
        result.amountOrDefault(new MatchingInvoiceAmounts.InvoiceKey(5L, march), BigDecimal.ZERO) == new BigDecimal("150.00")
        result.amountOrDefault(new MatchingInvoiceAmounts.InvoiceKey(5L, april), BigDecimal.ZERO) == new BigDecimal("50.00")
    }

    def "resolveMatchingInvoiceAmounts never mixes sums across different credit cards sharing the same reference month"() {
        given:
        LocalDate march = LocalDate.of(2026, 3, 1)
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, 7L, null, null, null)
        creditCardTransactionRepository.findByFilter(*_) >> [
                buildCreditCardItem(5L, march, 7L, new BigDecimal("120.00")),
                buildCreditCardItem(6L, march, 7L, new BigDecimal("999.00"))
        ]

        when:
        MatchingInvoiceAmounts result = matcher.resolveMatchingInvoiceAmounts(filter)

        then:
        result.amountOrDefault(new MatchingInvoiceAmounts.InvoiceKey(5L, march), BigDecimal.ZERO) == new BigDecimal("120.00")
        result.amountOrDefault(new MatchingInvoiceAmounts.InvoiceKey(6L, march), BigDecimal.ZERO) == new BigDecimal("999.00")
    }

    def "mergeWithMatchingInvoicePayments returns directMatches unchanged when matchingInvoiceAmounts is empty"() {
        given:
        List<Transaction> directMatches = [buildInvoicePaymentTransaction(1L, 5L, new BigDecimal("10.00"), LocalDate.now())]
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true, [:])
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, 7L, null, null, null)

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments(directMatches, matchingInvoiceAmounts, filter)

        then:
        result == directMatches
        0 * creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(*_)
    }

    def "mergeWithMatchingInvoicePayments adds a not-yet-included payment transaction that matches the non-category filters"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction paymentTransaction = buildInvoicePaymentTransaction(99L, 5L, new BigDecimal("450.00"), LocalDate.of(2026, 3, 15))
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, 7L, null, null, null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(5L, referenceMonth) >> buildInvoicePayment(5L, referenceMonth, 99L)
        transactionRepository.findById(99L) >> paymentTransaction

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments([], matchingInvoiceAmounts, filter)

        then:
        result.size() == 1
        result[0].getId() == 99L
    }

    def "mergeWithMatchingInvoicePayments does not duplicate a payment transaction already present in directMatches"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction paymentTransaction = buildInvoicePaymentTransaction(99L, 5L, new BigDecimal("450.00"), LocalDate.of(2026, 3, 15))
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, 7L, null, null, null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(5L, referenceMonth) >> buildInvoicePayment(5L, referenceMonth, 99L)

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments([paymentTransaction], matchingInvoiceAmounts, filter)

        then:
        result.size() == 1
        0 * transactionRepository.findById(*_)
    }

    def "mergeWithMatchingInvoicePayments skips a key whose invoice payment is not found"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, 7L, null, null, null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(5L, referenceMonth) >> null

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments([], matchingInvoiceAmounts, filter)

        then:
        result.isEmpty()
        0 * transactionRepository.findById(*_)
    }

    def "mergeWithMatchingInvoicePayments excludes a payment transaction whose bankAccountId does not match the filter"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction paymentTransaction = buildInvoicePaymentTransaction(99L, 5L, new BigDecimal("450.00"), LocalDate.of(2026, 3, 15))
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, 999L, 7L, null, null, null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(5L, referenceMonth) >> buildInvoicePayment(5L, referenceMonth, 99L)
        transactionRepository.findById(99L) >> paymentTransaction

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments([], matchingInvoiceAmounts, filter)

        then:
        result.isEmpty()
    }

    def "mergeWithMatchingInvoicePayments excludes a payment transaction whose transactionDate falls outside from/to"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction paymentTransaction = buildInvoicePaymentTransaction(99L, 5L, new BigDecimal("450.00"), LocalDate.of(2026, 3, 15))
        MatchingInvoiceAmounts matchingInvoiceAmounts = new MatchingInvoiceAmounts(true,
                [(new MatchingInvoiceAmounts.InvoiceKey(5L, referenceMonth)): new BigDecimal("120.00")])
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, null, 7L, null, null, null)
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(5L, referenceMonth) >> buildInvoicePayment(5L, referenceMonth, 99L)
        transactionRepository.findById(99L) >> paymentTransaction

        when:
        List<Transaction> result = matcher.mergeWithMatchingInvoicePayments([], matchingInvoiceAmounts, filter)

        then:
        result.isEmpty()
    }

    def "resolveInvoiceReferenceMonths returns an empty map and skips the repository when no transaction is an invoice payment"() {
        given:
        Transaction regular = buildInvoicePaymentTransaction(1L, 5L, BigDecimal.TEN, LocalDate.now())
        regular.setSourceType(null)
        regular.setSourceId(null)

        when:
        Map<Long, LocalDate> result = matcher.resolveInvoiceReferenceMonths([regular])

        then:
        result.isEmpty()
        0 * creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn(*_)
    }

    def "resolveInvoiceReferenceMonths maps payment transaction ids to their reference month"() {
        given:
        LocalDate referenceMonth = LocalDate.of(2026, 3, 1)
        Transaction paymentTransaction = buildInvoicePaymentTransaction(99L, 5L, new BigDecimal("450.00"), LocalDate.of(2026, 3, 15))
        creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn([99L]) >> [buildInvoicePayment(5L, referenceMonth, 99L)]

        when:
        Map<Long, LocalDate> result = matcher.resolveInvoiceReferenceMonths([paymentTransaction])

        then:
        result == [(99L): referenceMonth]
    }
}
