package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.LocalDate

class RecurrenceStartPolicySpec extends Specification {

    def "requireRecentStart accepts a start date up to exactly five years ago, or in the future"() {
        when:
        RecurrenceStartPolicy.requireRecentStart(startDate)

        then:
        noExceptionThrown()

        where:
        startDate << [LocalDate.now().minusYears(5), LocalDate.now(), LocalDate.now().plusYears(1)]
    }

    def "requireRecentStart rejects a start date more than five years ago"() {
        when:
        RecurrenceStartPolicy.requireRecentStart(startDate)

        then:
        DomainException exception = thrown()
        exception.message == "Start date cannot be more than 5 years in the past"

        where:
        startDate << [LocalDate.now().minusYears(5).minusDays(1), LocalDate.of(1, 1, 1)]
    }

    def "requireRecentStartIfChanged lets an old start date through only when it did not change"() {
        given:
        LocalDate oldStart = LocalDate.now().minusYears(6)

        when:
        RecurrenceStartPolicy.requireRecentStartIfChanged(oldStart, oldStart)

        then:
        noExceptionThrown()

        when:
        RecurrenceStartPolicy.requireRecentStartIfChanged(oldStart, oldStart.minusMonths(1))

        then:
        thrown(DomainException)
    }
}
