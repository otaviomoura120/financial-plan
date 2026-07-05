package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import spock.lang.Specification

import java.time.Instant

class DeleteCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    DeleteCategoryService service = new DeleteCategoryService(categoryRepository)

    def "execute deactivates the category instead of hard-deleting it"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(10L, 0, space, "Food", true, Instant.now(), null)
        categoryRepository.findById(10L) >> category

        when:
        service.execute(10L)

        then:
        1 * categoryRepository.update({ !it.isActive() })
    }
}
