package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant

class SubCategorySpec extends Specification {

    private Category buildCategory() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(1L, 0, space, "Food", true, Instant.now(), null)
    }

    def "validate passes when name and category are present"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "Groceries", true, Instant.now(), null)

        when:
        subCategory.validate()

        then:
        noExceptionThrown()
    }

    def "validate throws DomainException when name is blank"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "", true, Instant.now(), null)

        when:
        subCategory.validate()

        then:
        thrown(DomainException)
    }

    def "validate throws DomainException when category is null"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, null, "Groceries", true, Instant.now(), null)

        when:
        subCategory.validate()

        then:
        thrown(DomainException)
    }

    def "deactivate sets active to false"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "Groceries", true, Instant.now(), null)

        when:
        subCategory.deactivate()

        then:
        !subCategory.isActive()
    }

    def "activate sets active to true"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "Groceries", false, Instant.now(), null)

        when:
        subCategory.activate()

        then:
        subCategory.isActive()
    }
}
