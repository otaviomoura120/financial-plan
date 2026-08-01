package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant

class DeleteCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteCategoryService service = new DeleteCategoryService(categoryRepository, subCategoryRepository, transactionRepository)

    def setup() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        categoryRepository.findById(10L) >> new Category(10L, 0, space, "Food", true, Instant.now(), null)
    }

    def "execute hard-deletes the category when there are no subcategories or transactions linked to it"() {
        given:
        subCategoryRepository.existsByCategoryId(10L) >> false
        transactionRepository.existsByCategoryId(10L) >> false

        when:
        service.execute(10L)

        then:
        1 * categoryRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are subcategories linked to it"() {
        given:
        subCategoryRepository.existsByCategoryId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * categoryRepository.delete(_)
        0 * transactionRepository.existsByCategoryId(_)
    }

    def "execute throws DomainException and does not delete when there are transactions linked to it"() {
        given:
        subCategoryRepository.existsByCategoryId(10L) >> false
        transactionRepository.existsByCategoryId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * categoryRepository.delete(_)
    }


    def "execute throws DomainException and does not delete a system category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        categoryRepository.findById(20L) >> new Category(20L, 0, space, "Transferência", true, Instant.now(), null)

        when:
        service.execute(20L)

        then:
        thrown(DomainException)
        0 * categoryRepository.delete(_)
        0 * subCategoryRepository.existsByCategoryId(_)
    }
}
