package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.application.transaction.TransactionBalanceEffectService
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.enums.TransactionSourceType
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class UndoBillInstancePaymentServiceSpec extends Specification {

    BillInstanceRepository billInstanceRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    TransactionBalanceEffectService transactionBalanceEffectService = new TransactionBalanceEffectService(bankAccountRepository)

    UndoBillInstancePaymentService service = new UndoBillInstancePaymentService(billInstanceRepository, transactionRepository,
            transactionBalanceEffectService)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private Bill buildBill() {
        new Bill(10L, 0, buildSpace(), "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 3, 10), true, true, Instant.now(), null)
    }

    private BillInstance buildInstance(BillInstanceStatus status) {
        new BillInstance(1L, 0, buildBill(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), new BigDecimal("150.00"),
                status, LocalDate.of(2026, 3, 9), 99L, 2L, Instant.now(), null)
    }

    private BankAccount buildAccount(BigDecimal balance) {
        new BankAccount(2L, 0, buildSpace(), "Account", "BankCorp", balance, true, Instant.now(), null)
    }

    private Transaction buildPaymentTransaction(BankAccount bankAccount) {
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(30L, 0, null, "Contas", true, Instant.now(), null)
        PaymentMethod paymentMethod = new PaymentMethod(40L, 0, null, "Débito Automático", true, Instant.now(), null)
        new Transaction(99L, 0, TransactionType.EXPENSE, user, bankAccount, null, category, null, paymentMethod,
                new BigDecimal("150.00"), LocalDate.of(2026, 3, 9), "Pagamento de conta - Energy Bill", Instant.now(), null,
                TransactionSourceType.BILL_INSTANCE_PAYMENT, 10L)
    }

    def "execute reverts the account balance, deletes the transaction and reverts the instance to pending"() {
        given:
        BillInstance instance = buildInstance(BillInstanceStatus.PAID)
        billInstanceRepository.findById(1L) >> instance
        BankAccount account = buildAccount(new BigDecimal("350.00"))
        Transaction transaction = buildPaymentTransaction(account)
        transactionRepository.findById(99L) >> transaction
        bankAccountRepository.findById(2L) >> account

        when:
        service.execute(1L)

        then:
        account.getBalance() == new BigDecimal("500.00")
        1 * bankAccountRepository.update(_)
        1 * transactionRepository.delete(99L)
        1 * billInstanceRepository.update({ BillInstance i -> i.isPending() && i.getPaidDate() == null &&
                i.getPaymentTransactionId() == null && i.getBankAccountId() == null })
    }

    def "execute throws DomainException when the bill instance does not exist"() {
        given:
        billInstanceRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * transactionRepository.delete(_)
        0 * billInstanceRepository.update(_)
    }

    def "execute throws DomainException when the bill instance is not paid"() {
        given:
        billInstanceRepository.findById(1L) >> buildInstance(BillInstanceStatus.PENDING)

        when:
        service.execute(1L)

        then:
        thrown(DomainException)
        0 * transactionRepository.delete(_)
        0 * billInstanceRepository.update(_)
    }
}
