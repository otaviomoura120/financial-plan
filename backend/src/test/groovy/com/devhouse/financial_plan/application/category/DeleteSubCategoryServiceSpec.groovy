package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

class DeleteSubCategoryServiceSpec extends Specification {

    SubCategoryRepository subCategoryRepository = Mock()
    TransactionRepository transactionRepository = Mock()
    DeleteSubCategoryService service = new DeleteSubCategoryService(subCategoryRepository, transactionRepository)

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
}
