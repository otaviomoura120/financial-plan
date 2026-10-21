package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.domain.enums.CategoryKind
import com.devhouse.financial_plan.application.category.dto.CreateCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class CreateCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SpaceRepository spaceRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    CategoryNameValidator categoryNameValidator = new CategoryNameValidator(categoryRepository, subCategoryRepository)
    CreateCategoryService service = new CreateCategoryService(categoryRepository, spaceRepository, categoryNameValidator)

    def "execute creates category linked to space"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("Food", CategoryKind.EXPENSE)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category savedCategory = new Category(5L, null, space, "Food", CategoryKind.EXPENSE, true, Instant.now(), null)

        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> []
        categoryRepository.save(_) >> savedCategory

        when:
        CategoryResponse response = service.execute(1L, request)

        then:
        response.id() == 5L
        response.name() == "Food"
        response.kind() == CategoryKind.EXPENSE
        response.active()
    }

    def "execute saves the requested type"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("Salário", CategoryKind.INCOME)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> []

        when:
        CategoryResponse response = service.execute(1L, request)

        then:
        1 * categoryRepository.save({ Category c -> c.getKind() == CategoryKind.INCOME }) >> { Category c -> c }
        response.kind() == CategoryKind.INCOME
    }

    def "execute throws DomainException when the type is missing"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("Food", null)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> []

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }

    def "execute throws DomainException when space not found"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("Food", CategoryKind.EXPENSE)
        spaceRepository.findById(99L) >> null

        when:
        service.execute(99L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }

    def "execute throws DomainException when category name is blank"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("", CategoryKind.EXPENSE)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> []

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }


    def "execute throws DomainException when another category in the space already uses the name"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("  food ", CategoryKind.EXPENSE)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> [new Category(7L, 0, space, "Food", CategoryKind.EXPENSE, true, Instant.now(), null)]

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }

    def "execute throws DomainException when the name is reserved by the system"() {
        given:
        CreateCategoryRequest request = new CreateCategoryRequest("Pagamento de Fatura", CategoryKind.EXPENSE)
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        categoryRepository.findBySpaceId(1L) >> []

        when:
        service.execute(1L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }
}
