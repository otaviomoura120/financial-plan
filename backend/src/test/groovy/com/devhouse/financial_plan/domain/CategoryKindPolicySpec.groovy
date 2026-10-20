package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.CategoryKind
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class CategoryKindPolicySpec extends Specification {

    def "requireExpense accepts an expense category and an empty selection"() {
        when:
        CategoryKindPolicy.requireExpense(category)

        then:
        noExceptionThrown()

        where:
        category << [new Category(1L, 0, null, "Food", CategoryKind.EXPENSE, true, Instant.now(), null), null]
    }

    def "requireExpense rejects an income category"() {
        given:
        Category category = new Category(1L, 0, null, "Salário", CategoryKind.INCOME, true, Instant.now(), null)

        when:
        CategoryKindPolicy.requireExpense(category)

        then:
        DomainException exception = thrown()
        exception.message == "The selected category is not an expense category"
    }

    def "requireForCreditCardEntry expects income for an invoice credit and expense for a purchase"() {
        given:
        Category category = new Category(1L, 0, null, "Category", kind, true, Instant.now(), null)

        when:
        CategoryKindPolicy.requireForCreditCardEntry(category, credit)

        then:
        noExceptionThrown()

        where:
        credit | kind
        true   | CategoryKind.INCOME
        false  | CategoryKind.EXPENSE
    }

    def "requireForCreditCardEntry rejects the other kind"() {
        given:
        Category category = new Category(1L, 0, null, "Category", kind, true, Instant.now(), null)

        when:
        CategoryKindPolicy.requireForCreditCardEntry(category, credit)

        then:
        DomainException exception = thrown()
        exception.message == message

        where:
        credit | kind                 || message
        true   | CategoryKind.EXPENSE || "The selected category is not an income category"
        false  | CategoryKind.INCOME  || "The selected category is not an expense category"
    }

    def "requireForCreditCardEntry accepts an empty selection"() {
        when:
        CategoryKindPolicy.requireForCreditCardEntry(null, true)

        then:
        noExceptionThrown()
    }
}
