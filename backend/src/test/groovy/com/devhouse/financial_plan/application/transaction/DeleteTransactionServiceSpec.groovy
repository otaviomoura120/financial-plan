package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.application.billinstance.UndoBillInstancePaymentService
import com.devhouse.financial_plan.application.creditcardinvoice.UndoCreditCardInvoicePaymentService
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class DeleteTransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    TransactionBalanceEffectService balanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)
    UndoBillInstancePaymentService undoBillInstancePaymentService = Mock()
    UndoCreditCardInvoicePaymentService undoCreditCardInvoicePaymentService = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    DeleteTransactionService service = new DeleteTransactionService(transactionRepository, balanceEffectService,
            undoBillInstancePaymentService, undoCreditCardInvoicePaymentService, creditCardInvoicePaymentRepository)

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

    def "execute reverts the EXPENSE effect and deletes the transaction"() {
        given:
        Transaction transaction = new Transaction(1L, 0, TransactionType.EXPENSE, buildUser(1L),
                buildAccount(1L, BigDecimal.ZERO), null, buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null, null, null)
        transactionRepository.findById(1L) >> transaction
        BankAccount account = buildAccount(1L, new BigDecimal("400.00"))
        bankAccountRepository.findById(1L) >> account

        when:
        service.execute(1L)

        then:
        1 * bankAccountRepository.update(_)
        1 * transactionRepository.delete(1L)
        account.getBalance() == new BigDecimal("500.00")
    }

    def "execute reverts the TRANSFER effect on both accounts and deletes the transaction"() {
        given:
        Transaction transaction = new Transaction(2L, 0, TransactionType.TRANSFER, buildUser(1L),
                buildAccount(1L, BigDecimal.ZERO), buildAccount(2L, BigDecimal.ZERO), null, null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null, null, null)
        transactionRepository.findById(2L) >> transaction
        BankAccount origin = buildAccount(1L, new BigDecimal("400.00"))
        BankAccount destination = buildAccount(2L, new BigDecimal("300.00"))
        bankAccountRepository.findById(1L) >> origin
        bankAccountRepository.findById(2L) >> destination

        when:
        service.execute(2L)

        then:
        1 * transactionRepository.delete(2L)
        origin.getBalance() == new BigDecimal("500.00")
        destination.getBalance() == new BigDecimal("200.00")
    }

    def "execute throws DomainException when transaction does not exist"() {
        given:
        transactionRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * bankAccountRepository.update(_)
        0 * transactionRepository.delete(_)
    }

    def "execute delegates to UndoBillInstancePaymentService when linked to a BILL_INSTANCE_PAYMENT"() {
        given:
        Transaction transaction = new Transaction(3L, 0, TransactionType.EXPENSE, buildUser(1L),
                buildAccount(1L, BigDecimal.ZERO), null, buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null,
                TransactionSourceType.BILL_INSTANCE_PAYMENT, 50L)
        transactionRepository.findById(3L) >> transaction

        when:
        service.execute(3L)

        then:
        1 * undoBillInstancePaymentService.execute(50L)
        0 * undoCreditCardInvoicePaymentService.execute(_, _)
        0 * bankAccountRepository.update(_)
        0 * transactionRepository.delete(_)
    }

    def "execute delegates to UndoCreditCardInvoicePaymentService when linked to a CREDIT_CARD_INVOICE_PAYMENT"() {
        given:
        Transaction transaction = new Transaction(4L, 0, TransactionType.EXPENSE, buildUser(1L),
                buildAccount(1L, BigDecimal.ZERO), null, buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 60L)
        transactionRepository.findById(4L) >> transaction
        LocalDate referenceMonth = LocalDate.of(2026, 7, 1)
        CreditCardInvoicePayment payment = new CreditCardInvoicePayment(1L, 0, null, referenceMonth, referenceMonth,
                new BigDecimal("100.00"), LocalDate.now(), 4L, 1L, Instant.now(), null)
        creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn([4L]) >> [payment]

        when:
        service.execute(4L)

        then:
        1 * undoCreditCardInvoicePaymentService.execute(60L, referenceMonth)
        0 * undoBillInstancePaymentService.execute(_)
        0 * bankAccountRepository.update(_)
        0 * transactionRepository.delete(_)
    }

    def "execute throws DomainException when linked CREDIT_CARD_INVOICE_PAYMENT has no matching invoice payment"() {
        given:
        Transaction transaction = new Transaction(5L, 0, TransactionType.EXPENSE, buildUser(1L),
                buildAccount(1L, BigDecimal.ZERO), null, buildCategoryObj(10L), null,
                new BigDecimal("100.00"), LocalDate.now(), "desc", Instant.now(), null,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 60L)
        transactionRepository.findById(5L) >> transaction
        creditCardInvoicePaymentRepository.findByPaymentTransactionIdIn([5L]) >> []

        when:
        service.execute(5L)

        then:
        thrown(DomainException)
        0 * undoCreditCardInvoicePaymentService.execute(_, _)
    }
}
