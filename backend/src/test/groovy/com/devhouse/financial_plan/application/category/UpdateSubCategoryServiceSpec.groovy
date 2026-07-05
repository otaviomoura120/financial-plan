package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse
import com.devhouse.financial_plan.application.category.dto.UpdateSubCategoryRequest
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    UpdateSubCategoryService service = new UpdateSubCategoryService(subCategoryRepository)

    def "execute renames an existing subcategory"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, 1L, "Groceries", true, Instant.now(), null)
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "Supermarket")
        SubCategory updated = new SubCategory(10L, 0, 1L, "Supermarket", true, Instant.now(), Instant.now())

        subCategoryRepository.findById(10L) >> subCategory
        subCategoryRepository.update(_) >> updated

        when:
        SubCategoryResponse response = service.execute(10L, request)

        then:
        response.name() == "Supermarket"
        response.categoryId() == 1L
    }

    def "execute throws DomainException when new name is blank"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, 1L, "Groceries", true, Instant.now(), null)
        UpdateSubCategoryRequest request = new UpdateSubCategoryRequest(0, "")

        subCategoryRepository.findById(10L) >> subCategory

        when:
        service.execute(10L, request)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.update(_)
    }
}
