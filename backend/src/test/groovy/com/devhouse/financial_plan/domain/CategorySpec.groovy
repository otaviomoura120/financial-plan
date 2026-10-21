package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.enums.CategoryKind
import spock.lang.Specification

import java.time.Instant

class CategorySpec extends Specification {

    private Category buildCategory(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", CategoryKind.EXPENSE, active, Instant.now(), null)
    }

    def "validate throws DomainException when space is null"() {
        given:
        Category category = new Category(10L, 0, null, "Food", CategoryKind.EXPENSE, true, Instant.now(), null)

        when:
        category.validate()

        then:
        thrown(DomainException)
    }

    def "deactivate sets active to false"() {
        given:
        Category category = buildCategory(true)

        when:
        category.deactivate()

        then:
        !category.isActive()
    }

    def "activate sets active to true"() {
        given:
        Category category = buildCategory(false)

        when:
        category.activate()

        then:
        category.isActive()
    }


    def "isSystem is true only for reserved category names, ignoring case and surrounding blanks"() {
        expect:
        new Category(1L, 0, null, name, CategoryKind.EXPENSE, true, Instant.now(), null).isSystem() == expected

        where:
        name                    | expected
        "Pagamento de Fatura"   | true
        "  pagamento de fatura" | true
        "Transferência"         | true
        "TRANSFERÊNCIA"         | true
        "Transferencia"         | false
        "Alimentação"           | false
        null                    | false
    }

    def "hasSameName compares names ignoring case and surrounding blanks"() {
        given:
        Category category = new Category(1L, 0, null, "Alimentação", CategoryKind.EXPENSE, true, Instant.now(), null)

        expect:
        category.hasSameName("  alimentação ")
        !category.hasSameName("Alimentacao")
        !category.hasSameName(null)
    }

    def "validate throws DomainException when a user category has no type"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(10L, 0, space, "Food", null, true, Instant.now(), null)

        when:
        category.validate()

        then:
        DomainException exception = thrown()
        exception.message == "Category type is required"
    }

    def "validate accepts the transfer system category without a type"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(10L, 0, space, "Transferência", null, true, Instant.now(), null)

        when:
        category.validate()

        then:
        noExceptionThrown()
    }

    def "changeKind switches the type and rejects an empty one"() {
        given:
        Category category = buildCategory(true)

        when:
        category.changeKind(CategoryKind.INCOME)

        then:
        category.getKind() == CategoryKind.INCOME
        category.getUpdatedDate() != null

        when:
        category.changeKind(null)

        then:
        thrown(DomainException)
        category.getKind() == CategoryKind.INCOME
    }

    def "isKindChangedTo is true only for a different, non empty type"() {
        given:
        Category category = buildCategory(true)

        expect:
        category.isKindChangedTo(CategoryKind.INCOME)
        !category.isKindChangedTo(CategoryKind.EXPENSE)
        !category.isKindChangedTo(null)
    }

    def "requireKind accepts the same type and rejects the other one"() {
        given:
        Category category = buildCategory(true)

        when:
        category.requireKind(CategoryKind.EXPENSE)

        then:
        noExceptionThrown()

        when:
        category.requireKind(CategoryKind.INCOME)

        then:
        DomainException exception = thrown()
        exception.message == "The selected category is not an income category"
    }

    def "belongsToSpace is true only for the space the category is associated with"() {
        given:
        Category category = buildCategory(true)

        expect:
        category.belongsToSpace(1L)
        !category.belongsToSpace(2L)
    }
}
