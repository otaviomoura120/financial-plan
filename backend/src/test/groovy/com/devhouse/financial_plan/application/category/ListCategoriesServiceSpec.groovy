package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class ListCategoriesServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    ListCategoriesService service = new ListCategoriesService(categoryRepository, subCategoryRepository)

    def "execute returns categories of the space with their subcategories populated"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(10L, 0, space, "Food", true, Instant.now(), null)
        SubCategory subCategory = new SubCategory(20L, 0, 10L, "Restaurants", true, Instant.now(), null)
        categoryRepository.findBySpaceId(1L) >> [category]
        subCategoryRepository.findByCategoryId(10L) >> [subCategory]

        when:
        List<CategoryResponse> responses = service.execute(1L)

        then:
        responses.size() == 1
        responses[0].id() == 10L
        responses[0].name() == "Food"
        responses[0].subCategories().size() == 1
        responses[0].subCategories()[0].id() == 20L
        responses[0].subCategories()[0].name() == "Restaurants"
    }

    def "execute returns an empty list when the space has no categories"() {
        given:
        categoryRepository.findBySpaceId(99L) >> []

        when:
        List<CategoryResponse> responses = service.execute(99L)

        then:
        responses.isEmpty()
        0 * subCategoryRepository.findByCategoryId(_)
    }
}
