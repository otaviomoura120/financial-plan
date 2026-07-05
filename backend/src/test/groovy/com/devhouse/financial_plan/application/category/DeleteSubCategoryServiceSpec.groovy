package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class DeleteSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    DeleteSubCategoryService service = new DeleteSubCategoryService(subCategoryRepository)

    def "execute deactivates the subcategory instead of hard-deleting it"() {
        given:
        SubCategory subCategory = new SubCategory(10L, 0, 1L, "Groceries", true, Instant.now(), null)
        subCategoryRepository.findById(10L) >> subCategory

        when:
        service.execute(10L)

        then:
        !subCategory.isActive()
        1 * subCategoryRepository.update(subCategory)
    }
}
