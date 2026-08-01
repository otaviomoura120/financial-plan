package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.application.category.dto.UpdateCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    CategoryNameValidator categoryNameValidator = new CategoryNameValidator(categoryRepository, subCategoryRepository)
    UpdateCategoryService service = new UpdateCategoryService(categoryRepository, categoryNameValidator)

    private Category buildCategory() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", true, Instant.now(), null)
    }

    def "execute renames the category"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        categoryRepository.update(_) >> { Category c -> c }
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Groceries")

        when:
        CategoryResponse response = service.execute(10L, request)

        then:
        response.name() == "Groceries"
        response.subCategories().isEmpty()
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        UpdateCategoryRequest request = new UpdateCategoryRequest(99, "Groceries")

        when:
        service.execute(10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * categoryRepository.update(_)
    }


    def "execute allows renaming a category to its own current name"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        categoryRepository.update(_) >> { Category c -> c }
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Food")

        when:
        CategoryResponse response = service.execute(10L, request)

        then:
        response.name() == "Food"
    }

    def "execute throws DomainException when another category already uses the name, ignoring case"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category, new Category(11L, 0, space, "Transport", true, Instant.now(), null)]
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "TRANSPORT")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute throws DomainException when the category is a system category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        categoryRepository.findById(10L) >> new Category(10L, 0, space, "Pagamento de Fatura", true, Instant.now(), null)
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Outra coisa")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }
}
