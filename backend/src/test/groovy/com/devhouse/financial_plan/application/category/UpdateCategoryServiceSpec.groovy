package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.application.category.dto.UpdateCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    UpdateCategoryService service = new UpdateCategoryService(categoryRepository)

    private Category buildCategory() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", true, Instant.now(), null)
    }

    def "execute renames the category"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
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
}
