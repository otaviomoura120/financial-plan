package com.devhouse.financial_plan.application.report

import com.devhouse.financial_plan.application.billinstance.EnsureBillInstancesGeneratedService
import com.devhouse.financial_plan.application.creditcardinvoice.ListCreditCardInvoicesService
import com.devhouse.financial_plan.application.creditcardinvoice.dto.CreditCardInvoiceResponse
import com.devhouse.financial_plan.application.report.dto.ReportFilterRequest
import com.devhouse.financial_plan.application.report.dto.ReportResponse
import com.devhouse.financial_plan.domain.BankAccount
import com.devhouse.financial_plan.domain.Bill
import com.devhouse.financial_plan.domain.BillInstance
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.PaymentMethod
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.Transaction
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.BillInstanceStatus
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BankAccountRepository
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateReportServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    BankAccountRepository bankAccountRepository = Mock()
    ListCreditCardInvoicesService listCreditCardInvoicesService = Mock()
    EnsureBillInstancesGeneratedService ensureBillInstancesGeneratedService = Mock()
    BillInstanceRepository billInstanceRepository = Mock()

    GenerateReportService service = new GenerateReportService(transactionRepository, bankAccountRepository,
            listCreditCardInvoicesService, ensureBillInstancesGeneratedService, billInstanceRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private BankAccount buildAccount(Long id, BigDecimal balance = BigDecimal.ZERO, boolean active = true) {
        new BankAccount(id, 0, buildSpace(), "Account " + id, "BankCorp", balance, active, Instant.now(), null)
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

    private CreditCardInvoiceResponse buildInvoice(LocalDate dueDate, BigDecimal amount, boolean paid) {
        new CreditCardInvoiceResponse(5L, "Nubank", LocalDate.of(dueDate.year, dueDate.monthValue, 1), dueDate.minusDays(7),
                dueDate, amount, paid, paid ? dueDate : null, paid ? amount : null, paid ? 77L : null)
    }

    private BillInstance buildInstance(LocalDate dueDate, BigDecimal amount, BillInstanceStatus status) {
        Bill bill = new Bill(30L, 0, buildSpace(), "Energy Bill", null, new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10), true, true, Instant.now(), null)
        new BillInstance(40L, 0, bill, LocalDate.of(dueDate.year, dueDate.monthValue, 1), dueDate, amount, status,
                null, null, null, Instant.now(), null)
    }

    def "execute forwards every filter field to the repository, in the expected order"() {
        given:
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
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
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
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
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
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
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
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

    def "execute sums the balance of every active bank account in the space when bankAccountId is not filtered"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(1L) >> [buildAccount(1L, new BigDecimal("500.00")),
                                                     buildAccount(2L, new BigDecimal("300.00")),
                                                     buildAccount(3L, new BigDecimal("1000.00"), false)]
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.now(), LocalDate.now(), null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.currentBalance() == new BigDecimal("800.00")
        response.projectedBalance() == new BigDecimal("800.00")
    }

    def "execute uses only the filtered bank account's balance when bankAccountId is informed"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findById(2L) >> buildAccount(2L, new BigDecimal("300.00"))
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.now(), LocalDate.now(), null, 2L, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.currentBalance() == new BigDecimal("300.00")
        0 * bankAccountRepository.findBySpaceId(_)
    }

    def "execute includes an open credit card invoice due within the filtered period as pending"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(1L, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >>
                [buildInvoice(LocalDate.of(2026, 3, 17), new BigDecimal("450.00"), false)]
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.pendingCreditCardInvoices().size() == 1
        response.pendingCreditCardInvoices()[0].amount() == new BigDecimal("450.00")
        response.pendingCreditCardTotal() == new BigDecimal("450.00")
    }

    def "execute excludes a paid credit card invoice from the pending total"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(1L, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >>
                [buildInvoice(LocalDate.of(2026, 3, 17), new BigDecimal("450.00"), true)]
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.pendingCreditCardInvoices().isEmpty()
        response.pendingCreditCardTotal() == BigDecimal.ZERO
    }

    def "execute picks up a future installment's invoice automatically once its dueDate falls in the filtered period"() {
        given:
        // ListCreditCardInvoicesService already groups CreditCardTransaction rows by their stored referenceMonth,
        // so an installment several months in the future shows up here with no extra generation step.
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(1L, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)) >>
                [buildInvoice(LocalDate.of(2026, 6, 17), new BigDecimal("33.34"), false)]
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.pendingCreditCardInvoices()[0].amount() == new BigDecimal("33.34")
        response.pendingCreditCardTotal() == new BigDecimal("33.34")
    }

    def "execute ensures bill instances are generated up to the filtered 'to' date and includes pending ones due in the period"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >>
                [buildInstance(LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING)]
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        1 * ensureBillInstancesGeneratedService.execute(1L, LocalDate.of(2026, 3, 31))
        response.pendingBillInstances().size() == 1
        response.pendingBillInstances()[0].amount() == new BigDecimal("150.00")
        response.pendingBillTotal() == new BigDecimal("150.00")
    }

    def "execute excludes a paid bill instance from the pending total"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >>
                [buildInstance(LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PAID)]
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.pendingBillInstances().isEmpty()
        response.pendingBillTotal() == BigDecimal.ZERO
    }

    def "execute excludes a pending bill instance whose dueDate falls outside the filtered period"() {
        given:
        // findBySpaceAndPeriod already filters by dueDate at the repository level, so an out-of-range instance
        // never reaches the service — this test locks in that the service trusts the repository's filtering.
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.pendingBillInstances().isEmpty()
    }

    def "execute uses today as the bill-generation cap when 'to' is not informed"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(_) >> []
        listCreditCardInvoicesService.execute(*_) >> []
        billInstanceRepository.findBySpaceAndPeriod(*_) >> []
        ReportFilterRequest filter = new ReportFilterRequest(1L, null, null, null, null, null, null, null, null)

        when:
        service.execute(filter)

        then:
        1 * ensureBillInstancesGeneratedService.execute(1L, LocalDate.now())
    }

    def "execute computes projectedBalance as currentBalance minus pending credit card and bill totals"() {
        given:
        transactionRepository.findByFilter(*_) >> []
        bankAccountRepository.findBySpaceId(1L) >> [buildAccount(1L, new BigDecimal("1000.00"))]
        listCreditCardInvoicesService.execute(*_) >> [buildInvoice(LocalDate.of(2026, 3, 17), new BigDecimal("450.00"), false)]
        billInstanceRepository.findBySpaceAndPeriod(*_) >> [buildInstance(LocalDate.of(2026, 3, 10), new BigDecimal("150.00"), BillInstanceStatus.PENDING)]
        ReportFilterRequest filter = new ReportFilterRequest(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                null, null, null, null, null, null)

        when:
        ReportResponse response = service.execute(filter)

        then:
        response.currentBalance() == new BigDecimal("1000.00")
        response.pendingCreditCardTotal() == new BigDecimal("450.00")
        response.pendingBillTotal() == new BigDecimal("150.00")
        response.projectedBalance() == new BigDecimal("400.00")
    }
}
