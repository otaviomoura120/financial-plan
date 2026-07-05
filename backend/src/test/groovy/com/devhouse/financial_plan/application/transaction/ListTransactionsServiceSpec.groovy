package com.devhouse.financial_plan.application.transaction

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class ListTransactionsServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    ListTransactionsService service = new ListTransactionsService(transactionRepository)

    def "execute forwards spaceId and optional filters to the repository"() {
        given:
        Transaction transaction = new Transaction(1L, 0, TransactionType.EXPENSE, 1L, 1L, null, 10L, null, 20L,
                new BigDecimal("100.00"), LocalDate.of(2026, 1, 15), "desc", Instant.now(), null)
        transactionRepository.findByFilter(1L, null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) >> [transaction]

        when:
        List<TransactionResponse> responses = service.execute(1L, null, null, null, null, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))

        then:
        responses.size() == 1
        responses[0].id() == 1L
        responses[0].type() == TransactionType.EXPENSE
    }

    def "execute returns an empty list when the space has no matching transactions"() {
        given:
        transactionRepository.findByFilter(*_) >> []

        when:
        List<TransactionResponse> responses = service.execute(99L, null, null, null, null, null, null, null, null)

        then:
        responses.isEmpty()
    }
}
