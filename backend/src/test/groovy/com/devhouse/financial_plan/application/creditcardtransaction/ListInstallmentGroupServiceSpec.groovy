package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransaction
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListInstallmentGroupServiceSpec extends Specification {

    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    ListInstallmentGroupService service = new ListInstallmentGroupService(creditCardTransactionRepository)

    private CreditCardTransaction buildInstallment(Integer installmentNumber) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        CreditCard creditCard = new CreditCard(10L, 0, space, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
        User user = new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
        Category category = new Category(20L, 0, null, "Food", true, Instant.now(), null)
        new CreditCardTransaction(installmentNumber as Long, 0, creditCard, user, category, null,
                new BigDecimal("33.33"), LocalDate.of(2026, 3, 5), "desc",
                LocalDate.of(2026, 3, 1).plusMonths(installmentNumber - 1),
                "group-1", installmentNumber, 3, false, null, Instant.now(), null)
    }

    def "execute returns every installment of the group sorted by installment number"() {
        given:
        creditCardTransactionRepository.findByInstallmentGroupId("group-1") >> [buildInstallment(3), buildInstallment(1), buildInstallment(2)]

        when:
        List<CreditCardTransactionResponse> responses = service.execute("group-1")

        then:
        responses.size() == 3
        responses[0].installmentNumber() == 1
        responses[1].installmentNumber() == 2
        responses[2].installmentNumber() == 3
    }

    def "execute returns an empty list when the group does not exist"() {
        given:
        creditCardTransactionRepository.findByInstallmentGroupId("missing") >> []

        when:
        List<CreditCardTransactionResponse> responses = service.execute("missing")

        then:
        responses.isEmpty()
    }
}
