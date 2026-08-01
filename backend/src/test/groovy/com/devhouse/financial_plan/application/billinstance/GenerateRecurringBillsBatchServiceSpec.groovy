package com.devhouse.financial_plan.application.billinstance

import com.devhouse.financial_plan.domain.BillRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class GenerateRecurringBillsBatchServiceSpec extends Specification {

    BillRecurringRepository billRecurringRepository = Mock()
    EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService = Mock()

    GenerateRecurringBillsBatchService service =
            new GenerateRecurringBillsBatchService(billRecurringRepository, ensureRecurringBillsGeneratedService)

    private BillRecurring buildBillRecurring(Long id) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new BillRecurring(id, 0, space, "Energy Bill", null, null, new BigDecimal("150.00"), LocalDate.of(2026, 3, 10),
                null, null, true, Instant.now(), null)
    }

    def "execute tops up every active recurrence"() {
        given:
        BillRecurring first = buildBillRecurring(10L)
        BillRecurring second = buildBillRecurring(11L)
        billRecurringRepository.findAllActive() >> [first, second]

        when:
        service.execute()

        then:
        1 * ensureRecurringBillsGeneratedService.executeForRecurring(first)
        1 * ensureRecurringBillsGeneratedService.executeForRecurring(second)
    }

    def "execute keeps processing the batch when one recurrence fails"() {
        given:
        BillRecurring failing = buildBillRecurring(10L)
        BillRecurring healthy = buildBillRecurring(11L)
        billRecurringRepository.findAllActive() >> [failing, healthy]
        ensureRecurringBillsGeneratedService.executeForRecurring(failing) >> { throw new IllegalStateException("boom") }

        when:
        service.execute()

        then:
        notThrown(Exception)
        1 * ensureRecurringBillsGeneratedService.executeForRecurring(healthy)
    }
}
