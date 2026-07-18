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
}
