package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest
import com.devhouse.financial_plan.application.report.dto.ReportResponse
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateReportServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    GenerateReportService service = new GenerateReportService(transactionRepository)

    private Transaction buildTransaction(TransactionType type, BigDecimal amount, Long destinationBankAccountId = null) {
        Long categoryId = TransactionType.TRANSFER.equals(type) ? null : 10L
        Long paymentMethodId = TransactionType.TRANSFER.equals(type) ? null : 20L
        new Transaction(1L, 0, type, 1L, 1L, destinationBankAccountId, categoryId, null, paymentMethodId,
                amount, LocalDate.now(), "desc", Instant.now(), null)
    }

    def "execute forwards every filter field to the repository, in the expected order"() {
        given:
        ReportFilterRequest filter = new ReportFilterRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                1L, 2L, 3L, 4L, 5L, TransactionType.EXPENSE)

        when:
        service.execute(filter)

        then:
        1 * transactionRepository.findByFilter(1L, 2L, 3L, 4L, 5L, TransactionType.EXPENSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)) >> []
    }

    def "execute sums INCOME and EXPENSE separately and excludes TRANSFER from the totals"() {
        given:
        Transaction income = buildTransaction(TransactionType.INCOME, new BigDecimal("300.00"))
        Transaction expense = buildTransaction(TransactionType.EXPENSE, new BigDecimal("100.00"))
        Transaction transfer = buildTransaction(TransactionType.TRANSFER, new BigDecimal("500.00"), 2L)
        transactionRepository.findByFilter(*_) >> [income, expense, transfer]
        ReportFilterRequest filter = new ReportFilterRequest(LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

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
        ReportFilterRequest filter = new ReportFilterRequest(LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.transactions().isEmpty()
        response.totalIncome() == BigDecimal.ZERO
        response.totalExpense() == BigDecimal.ZERO
        response.balance() == BigDecimal.ZERO
    }
}
