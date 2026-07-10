package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreditCardInvoicePaymentResolverSpec extends Specification {

    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    CreditCardInvoicePaymentResolver resolver = new CreditCardInvoicePaymentResolver(creditCardInvoicePaymentRepository)

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
        new CreditCard(id, 0, buildSpace(), null, "Card " + id, new BigDecimal("1000.00"), 20, 27, true, Instant.now(), null)
    }

    private Transaction buildInvoicePaymentTransaction(Long id, Long creditCardId, BigDecimal amount, LocalDate transactionDate) {
        Category category = new Category(10L, 0, null, "Cartão de Crédito", true, Instant.now(), null)
        new Transaction(id, 0, TransactionType.EXPENSE, buildUser(1L), buildAccount(1L), null, category, null,
                amount, transactionDate, "Pagamento de fatura", Instant.now(), null,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, creditCardId)
    }

    private CreditCardInvoicePayment buildInvoicePayment(Long creditCardId, LocalDate referenceMonth, Long paymentTransactionId) {
        new CreditCardInvoicePayment(1L, 0, buildCreditCard(creditCardId), referenceMonth, referenceMonth.plusDays(10),
                new BigDecimal("450.00"), referenceMonth.plusDays(10), paymentTransactionId, 1L, Instant.now(), null)
    }

    def "resolveInvoiceReferenceMonths returns an empty map and skips the repository when no transaction is an invoice payment"() {
        given:
        Transaction regular = buildInvoicePaymentTransaction(1L, 5L, BigDecimal.TEN, LocalDate.now())
        regular.setSourceType(null)
        regular.setSourceId(null)

        when:
        Map<Long, LocalDate> result = resolver.resolveInvoiceReferenceMonths([regular])

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
        Map<Long, LocalDate> result = resolver.resolveInvoiceReferenceMonths([paymentTransaction])

        then:
        result == [(99L): referenceMonth]
    }
}
