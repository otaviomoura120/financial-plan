package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant

class DeleteSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteSubCategoryService service = new DeleteSubCategoryService(subCategoryRepository, transactionRepository)

    def setup() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(1L, 0, space, "Food", true, Instant.now(), null)
        subCategoryRepository.findById(10L) >> new SubCategory(10L, 0, category, "Groceries", true, Instant.now(), null)
    }

    def "execute hard-deletes the subcategory when there are no linked transactions"() {
        given:
        transactionRepository.existsBySubCategoryId(10L) >> false

        when:
        service.execute(10L)

        then:
        1 * subCategoryRepository.delete(10L)
    }

    def "execute throws DomainException and does not delete when there are linked transactions"() {
        given:
        transactionRepository.existsBySubCategoryId(10L) >> true

        when:
        service.execute(10L)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.delete(_)
    }


    def "execute throws DomainException and does not delete a subcategory of a system category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category systemCategory = new Category(1L, 0, space, "Pagamento de Fatura", true, Instant.now(), null)
        subCategoryRepository.findById(20L) >> new SubCategory(20L, 0, systemCategory, "Fatura de Cartão", true, Instant.now(), null)

        when:
        service.execute(20L)

        then:
        thrown(DomainException)
        0 * subCategoryRepository.delete(_)
        0 * transactionRepository.existsBySubCategoryId(_)
    }
}
