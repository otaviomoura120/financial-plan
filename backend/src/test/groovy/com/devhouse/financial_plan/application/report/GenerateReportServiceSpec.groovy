package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest
import com.devhouse.financial_plan.application.report.dto.ReportResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateReportServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    GenerateReportService service = new GenerateReportService(transactionRepository)

    private BankAccount buildAccount(Long id) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BankAccount(id, 0, space, "Account " + id, "BankCorp", BigDecimal.ZERO, true, Instant.now(), null)
    }

    private User buildUser(Long id) {
        new User(id, 0, "auth0|" + id, "User " + id, null, null, null, null, "user${id}@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    private Transaction buildTransaction(TransactionType type, BigDecimal amount, Long destinationBankAccountId = null) {
        Category category = TransactionType.TRANSFER.equals(type) ? null : new Category(10L, 0, null, "Food", true, Instant.now(), null)
        PaymentMethod paymentMethod = TransactionType.TRANSFER.equals(type) ? null : new PaymentMethod(20L, 0, null, "Cash", true, Instant.now(), null)
        new Transaction(1L, 0, type, buildUser(1L), buildAccount(1L),
                destinationBankAccountId != null ? buildAccount(destinationBankAccountId) : null,
                category, null, paymentMethod, amount, LocalDate.now(), "desc", Instant.now(), null, null, null)
    }

    def "execute forwards every filter field to the repository, in the expected order"() {
        given:
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                2L, 3L, 4L, 5L, 6L, TransactionType.EXPENSE)

        when:
        service.execute(filter)

        then:
        1 * transactionRepository.findByFilter(1L, 2L, 3L, 4L, 5L, 6L, TransactionType.EXPENSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) >> []
    }

    def "execute sums INCOME and EXPENSE separately and excludes TRANSFER from the totals"() {
        given:
        Transaction income = buildTransaction(TransactionType.INCOME, new BigDecimal("300.00"))
        Transaction expense = buildTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"))
        Transaction transfer = buildTransaction(TransactionType.TRANSFER, new BigDecimal("500.00"), 2L)
        transactionRepository.findByFilter(*_) >> [income, expense, transfer]
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.transactions().size() == 3
        response.transactions()*.type().contains(TransactionType.TRANSFER)
        response.totalIncome() == new BigDecimal("300.00")
        response.totalExpense() == new BigDecimal("100.00")
        response.balance() == new BigDecimal("200.00")
    }

    def "execute returns zero totals when there are no transactions in the period"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.transactions().isEmpty()
        response.totalIncome() == BigDecimal.ZERO
        response.totalExpense() == BigDecimal.ZERO
        response.balance() == BigDecimal.ZERO
    }

    def "execute throws DomainException when spaceId is missing"() {
        given:
        ReportFilterRequest filter = new ReportFilterRequest(null, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        service.execute(filter)

        then:
        thrown(DomainException)
        0 * transactionRepository.findByFilter(*_)
    }

    def "execute isolates results per space and never mixes totals across spaces"() {
        given:
        Transaction spaceOneIncome = buildTransaction(TransactionType.INCOME, new BigDecimal("100.00"))
        transactionRepository.findByFilter(1L, _, _, _, _, _, _, _, _) >> [spaceOneIncome]
        transactionRepository.findByFilter(2L, _, _, _, _, _, _, _, _) >> []
        ReportFilterRequest filterSpaceOne = new ReportFilterRequest(1L, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)
        ReportFilterRequest filterSpaceTwo = new ReportFilterRequest(2L, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        ReportResponse responseSpaceOne = service.execute(filterSpaceOne)
        ReportResponse responseSpaceTwo = service.execute(filterSpaceTwo)

        then:
        responseSpaceOne.transactions().size() == 1
        responseSpaceOne.totalIncome() == new BigDecimal("100.00")
        responseSpaceTwo.transactions().isEmpty()
        responseSpaceTwo.totalIncome() == BigDecimal.ZERO
    }
}
