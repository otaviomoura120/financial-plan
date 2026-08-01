package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class CategorySpec extends Specification {

    private Category buildCategory(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", active, Instant.now(), null)
    }

    def "validate throws DomainException when space is null"() {
        given:
        Category category = new Category(10L, 0, null, "Food", true, Instant.now(), null)

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
        new Category(1L, 0, null, name, true, Instant.now(), null).isSystem() == expected

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
        Category category = new Category(1L, 0, null, "Alimentação", true, Instant.now(), null)

        expect:
        category.hasSameName("  alimentação ")
        !category.hasSameName("Alimentacao")
        !category.hasSameName(null)
    }
}
