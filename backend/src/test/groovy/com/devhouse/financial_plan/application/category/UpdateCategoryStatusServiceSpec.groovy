package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCategoryStatusServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    UpdateCategoryStatusService service = new UpdateCategoryStatusService(categoryRepository)

    private Category buildCategory(boolean active) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", active, Instant.now(), null)
    }

    def "execute activates an inactive category"() {
        given:
        Category category = buildCategory(false)
        categoryRepository.findById(10L) >> category

        when:
        CategoryResponse response = service.execute(10L, true)

        then:
        response.active()
        1 * categoryRepository.update({ it.isActive() }) >> { Category c -> c }
    }

    def "execute deactivates an active category"() {
        given:
        Category category = buildCategory(true)
        categoryRepository.findById(10L) >> category

        when:
        CategoryResponse response = service.execute(10L, false)

        then:
        !response.active()
        1 * categoryRepository.update({ !it.isActive() }) >> { Category c -> c }
    }
}
