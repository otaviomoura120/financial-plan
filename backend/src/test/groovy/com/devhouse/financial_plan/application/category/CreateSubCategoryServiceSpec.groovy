package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CreateSubCategoryRequest
import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class CreateSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    CategoryNameValidator categoryNameValidator = new CategoryNameValidator(categoryRepository, subCategoryRepository)
    CreateSubCategoryService service = new CreateSubCategoryService(subCategoryRepository, categoryRepository,
            categoryNameValidator)

    def "execute creates subcategory linked to an existing category"() {
        given:
        CreateSubCategoryRequest request = new CreateSubCategoryRequest(1L, "Groceries")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(1L, 0, space, "Food", true, Instant.now(), null)
        SubCategory saved = new SubCategory(10L, 0, category, "Groceries", true, Instant.now(), null)

        categoryRepository.findById(1L) >> category
        subCategoryRepository.findByCategoryId(1L) >> []
        subCategoryRepository.save(_) >> saved

        when:
        SubCategoryResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.categoryId() == 1L
        response.name() == "Groceries"
        response.active()
    }

    def "execute throws DomainException when parent category is not found"() {
        given:
        CreateSubCategoryRequest request = new CreateSubCategoryRequest(99L, "Groceries")
        categoryRepository.findById(99L) >> null

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.save(_)
    }


    def "execute throws DomainException when the category already has a subcategory with the same name"() {
        given:
        CreateSubCategoryRequest request = new CreateSubCategoryRequest(1L, "groceries")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(1L, 0, space, "Food", true, Instant.now(), null)
        categoryRepository.findById(1L) >> category
        subCategoryRepository.findByCategoryId(1L) >> [new SubCategory(5L, 0, category, "Groceries", true, Instant.now(), null)]

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.save(_)
    }

    def "execute throws DomainException when the parent category is a system category"() {
        given:
        CreateSubCategoryRequest request = new CreateSubCategoryRequest(1L, "Anything")
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        categoryRepository.findById(1L) >> new Category(1L, 0, space, "Pagamento de Fatura", true, Instant.now(), null)

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.save(_)
    }
}
