package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.application.category.dto.CreateCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import spock.lang.Specification

import java.time.Instant

class CreateCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    CreateCategoryService service = new CreateCategoryService(categoryRepository, spaceRepository)

    def "execute creates category linked to space"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest(1L, "Food")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category savedCategory = new Category(5L, null, space, "Food", true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        categoryRepository.save(_) >> savedCategory

        when:
        CategoryResponse response = service.execute(request)

        then:
        response.id() == 5L
        response.name() == "Food"
        response.active()
    }

    def "execute throws DomainException when space not found"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest(99L, "Food")
        spaceRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }

    def "execute throws DomainException when category name is blank"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest(1L, "")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }
}
