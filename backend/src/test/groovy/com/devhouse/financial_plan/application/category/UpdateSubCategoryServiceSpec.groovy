package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse
import com.devhouse.financial_plan.application.category.dto.UpdateSubCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    CategoryNameValidator categoryNameValidator = new CategoryNameValidator(categoryRepository, subCategoryRepository)
    UpdateSubCategoryService service = new UpdateSubCategoryService(subCategoryRepository, categoryNameValidator)

    private Category buildCategory() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(1L, 0, space, "Food", true, Instant.now(), null)
    }

    def "execute renames an existing subcategory"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "Groceries", true, Instant.now(), null)
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "Supermarket")
        SubCategory updated = new SubCategory(10L, 0, buildCategory(), "Supermarket", true, Instant.now(), Instant.now())

        subCategoryRepository.findById(10L) >> subCategory
        subCategoryRepository.findByCategoryId(1L) >> [subCategory]
        subCategoryRepository.update(_) >> updated

        when:
        SubCategoryResponse response = service.execute(10L, request)

        then:
        response.name() == "Supermarket"
        response.categoryId() == 1L
    }

    def "execute throws DomainException when new name is blank"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, buildCategory(), "Groceries", true, Instant.now(), null)
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "")

        subCategoryRepository.findById(10L) >> subCategory
        subCategoryRepository.findByCategoryId(1L) >> [subCategory]

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.update(_)
    }


    def "execute throws DomainException when a sibling subcategory already uses the name"() {
        given:
        Category category = buildCategory()
        SubCategory subCategory = new SubCategory(10L, 0, category, "Groceries", true, Instant.now(), null)
        subCategoryRepository.findById(10L) >> subCategory
        subCategoryRepository.findByCategoryId(1L) >> [subCategory,
                new SubCategory(11L, 0, category, "Restaurants", true, Instant.now(), null)]
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "restaurants")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.update(_)
    }

    def "execute throws DomainException when the subcategory belongs to a system category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category systemCategory = new Category(1L, 0, space, "Pagamento de Fatura", true, Instant.now(), null)
        subCategoryRepository.findById(10L) >> new SubCategory(10L, 0, systemCategory, "Fatura de Cartão", true, Instant.now(), null)
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "Outro nome")

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.update(_)
    }
}
