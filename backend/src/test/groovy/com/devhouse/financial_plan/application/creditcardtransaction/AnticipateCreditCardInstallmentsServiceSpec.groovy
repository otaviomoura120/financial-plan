package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardInvoicePayment
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class AnticipateCreditCardInstallmentsServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository = Mock()

    AnticipateCreditCardInstallmentsService service = new AnticipateCreditCardInstallmentsService(
            creditCardTransactionRepository, creditCardInvoicePaymentRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(10L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Category buildCategory() {
        new Category(20L, 0, null, "Food", true, Instant.now(), null)
    }

    private CreditCardTransaction buildInstallment(Integer number, Integer total, LocalDate referenceMonth,
                                                     boolean anticipated = false, LocalDate originalReferenceMonth = null) {
        new CreditCardTransaction(number as Long, 0, buildCreditCard(), null, buildUser(), buildCategory(), null,
                new BigDecimal("100.00"), false, LocalDate.of(2026, 1, 5), "desc", referenceMonth,
                "group-1", number, total, anticipated, originalReferenceMonth, Instant.now(), null)
    }

    def "execute anticipates exactly the last N eligible installments and preserves the intermediate ones"() {
        given:
        List<CreditCardTransaction> group = [
                buildInstallment(1, 4, LocalDate.of(2026, 1, 1)),
                buildInstallment(2, 4, LocalDate.of(2026, 2, 1)),
                buildInstallment(3, 4, LocalDate.of(2026, 3, 1)),
                buildInstallment(4, 4, LocalDate.of(2026, 4, 1))
        ]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> group
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> null

        when:
        List<CreditCardTransactionResponse> responses = service.execute("group-1", LocalDate.of(2026, 1, 1), 2)

        then:
        2 * creditCardTransactionRepository.update(_)
        responses.size() == 4
        responses[0].referenceMonth() == LocalDate.of(2026, 1, 1)
        responses[0].competenceMonth() == LocalDate.of(2026, 1, 1)
        !responses[0].anticipated()
        responses[1].referenceMonth() == LocalDate.of(2026, 2, 1)
        responses[1].competenceMonth() == LocalDate.of(2026, 2, 1)
        !responses[1].anticipated()
        responses[2].referenceMonth() == LocalDate.of(2026, 1, 1)
        responses[2].competenceMonth() == LocalDate.of(2026, 1, 1)
        responses[2].anticipated()
        responses[2].originalReferenceMonth() == LocalDate.of(2026, 3, 1)
        responses[3].referenceMonth() == LocalDate.of(2026, 1, 1)
        responses[3].competenceMonth() == LocalDate.of(2026, 1, 1)
        responses[3].anticipated()
        responses[3].originalReferenceMonth() == LocalDate.of(2026, 4, 1)
        responses.every { it.totalAmount() == new BigDecimal("400.00") }
    }

    def "execute throws DomainException when the installment group does not exist"() {
        given:
        creditCardTransactionRepository.findByInstallmentGroupId("missing") >> []

        when:
        service.execute("missing", LocalDate.of(2026, 1, 1), 1)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws DomainException when the target invoice is already paid"() {
        given:
        List<CreditCardTransaction> group = [
                buildInstallment(1, 2, LocalDate.of(2026, 1, 1)),
                buildInstallment(2, 2, LocalDate.of(2026, 2, 1))
        ]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> group
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> Mock(CreditCardInvoicePayment)

        when:
        service.execute("group-1", LocalDate.of(2026, 1, 1), 1)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws DomainException when installmentsToAnticipate exceeds the eligible installments"() {
        given:
        List<CreditCardTransaction> group = [
                buildInstallment(1, 2, LocalDate.of(2026, 1, 1)),
                buildInstallment(2, 2, LocalDate.of(2026, 2, 1))
        ]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> group
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> null

        when:
        service.execute("group-1", LocalDate.of(2026, 1, 1), 5)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)
    }

    def "execute throws DomainException when installmentsToAnticipate is null or not positive"() {
        given:
        List<CreditCardTransaction> group = [buildInstallment(1, 1, LocalDate.of(2026, 1, 1))]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> group

        when:
        service.execute("group-1", LocalDate.of(2026, 1, 1), installmentsToAnticipate)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRepository.update(_)

        where:
        installmentsToAnticipate << [null, 0, -1]
    }

    def "execute does not overwrite originalReferenceMonth when anticipating an already-anticipated installment again"() {
        given:
        List<CreditCardTransaction> group = [
                buildInstallment(1, 3, LocalDate.of(2026, 1, 1)),
                buildInstallment(2, 3, LocalDate.of(2026, 2, 1)),
                buildInstallment(3, 3, LocalDate.of(2026, 2, 1), true, LocalDate.of(2026, 3, 1))
        ]
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> group
        creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(10L, LocalDate.of(2026, 1, 1)) >> null

        when:
        List<CreditCardTransactionResponse> responses = service.execute("group-1", LocalDate.of(2026, 1, 1), 1)

        then:
        1 * creditCardTransactionRepository.update(_)
        responses[2].referenceMonth() == LocalDate.of(2026, 1, 1)
        responses[2].anticipated()
        responses[2].originalReferenceMonth() == LocalDate.of(2026, 3, 1)
        responses[1].referenceMonth() == LocalDate.of(2026, 2, 1)
        !responses[1].anticipated()
    }
}
