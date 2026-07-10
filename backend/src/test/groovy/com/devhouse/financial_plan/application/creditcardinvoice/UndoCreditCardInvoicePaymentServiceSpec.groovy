package com.devhouse.financial_plan.application.creditcardinvoice

import com.devhouse.financial_plan.application.transaction.TransactionBalanceEffectService
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.PaymentMethod
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

class UndoCreditCardInvoicePaymentServiceSpec extends Specification {

    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    TransactionBalanceEffectService transactionBalanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    UndoCreditCardInvoicePaymentService service = new UndoCreditCardInvoicePaymentService(creditCardInvoicePaymentRepository,
            transactionRepository, transactionBalanceEffectService)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private CreditCardInvoicePayment buildPayment() {
        new CreditCardInvoicePayment(5L, 0, buildCreditCard(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 17),
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), 99L, 2L, Instant.now(), null)
    }

    private Transaction buildPaymentTransaction(BankAccount bankAccount) {
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(30L, 0, null, "Cartão", true, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(40L, 0, null, "Débito Automático", true, Instant.now(), null)
        new Transaction(99L, 0, TransactionType.EXPENSE, user, bankAccount, null, category, null, paymentMethod,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 10), "Pagamento de fatura - Nubank", Instant.now(), null,
                TransactionSourceType.CREDIT_CARD_INVOICE_PAYMENT, 10L)
    }

    private BankAccount buildAccount(BigDecimal balance) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(2L, 0, space, "Account", "BankCorp", balance, true, Instant.now(), null)
    }

    def "execute reverts the account balance, deletes the transaction and the payment"() {
        given:
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> buildPayment()
        BankAccount account = buildAccount(new BigDecimal("350.00"))
        Transaction transaction = buildPaymentTransaction(account)
        transactionRepository.findById(99L) >> transaction
        bankAccountRepository.findById(2L) >> account

        when:
        service.execute(10L, LocalDate.of(2026, 3, 1))

        then:
        account.getBalance() == new BigDecimal("500.00")
        1 * bankAccountRepository.update(_)
        1 * transactionRepository.delete(99L)
        1 * creditCardInvoicePaymentRepository.deleteById(5L)
    }

    def "execute throws DomainException when the invoice payment does not exist"() {
        given:
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 3, 1)) >> null

        when:
        service.execute(10L, LocalDate.of(2026, 3, 1))

        then:
        thrown(DomainException)
        0 * transactionRepository.delete(_)
        0 * creditCardInvoicePaymentRepository.deleteById(_)
    }
}
