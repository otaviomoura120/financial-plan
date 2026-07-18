package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

class DeleteCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteCategoryService service = new DeleteCategoryService(categoryRepository, subCategoryRepository, transactionRepository)

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
}
