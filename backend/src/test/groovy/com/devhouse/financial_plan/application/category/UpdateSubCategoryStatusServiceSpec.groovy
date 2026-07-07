package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.SubCategoryResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateSubCategoryStatusServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    UpdateSubCategoryStatusService service = new UpdateSubCategoryStatusService(subCategoryRepository)

    private SubCategory buildSubCategory(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(1L, 0, space, "Food", true, Instant.now(), null)
        new SubCategory(10L, 0, category, "Groceries", active, Instant.now(), null)
    }

    def "execute activates an inactive subcategory"() {
        given:
        SubCategory subCategory = buildSubCategory(false)
        subCategoryRepository.findById(10L) >> subCategory

        when:
        SubCategoryResponse response = service.execute(10L, true)

        then:
        response.active()
        1 * subCategoryRepository.update({ it.isActive() }) >> { SubCategory s -> s }
    }

    def "execute deactivates an active subcategory"() {
        given:
        SubCategory subCategory = buildSubCategory(true)
        subCategoryRepository.findById(10L) >> subCategory

        when:
        SubCategoryResponse response = service.execute(10L, false)

        then:
        !response.active()
        1 * subCategoryRepository.update({ !it.isActive() }) >> { SubCategory s -> s }
    }
}
