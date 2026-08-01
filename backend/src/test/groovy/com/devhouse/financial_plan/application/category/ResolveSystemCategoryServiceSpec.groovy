package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.SystemCategoryPair
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.enums.SystemCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class ResolveSystemCategoryServiceSpec extends Specification {

    EnsureSystemCategoriesService ensureSystemCategoriesService = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    ResolveSystemCategoryService service = new ResolveSystemCategoryService(ensureSystemCategoriesService,
            categoryRepository, subCategoryRepository)

    private Category buildCategory(Long id, String name) {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(id, 0, space, name, true, Instant.now(), null)
    }

    def "execute provisions the categories before resolving and returns both ids"() {
        given:
        Category invoiceCategory = buildCategory(51L, "Pagamento de Fatura")
        categoryRepository.findBySpaceId(1L) >> [buildCategory(9L, "Food"), invoiceCategory]
        subCategoryRepository.findByCategoryId(51L) >> [new SubCategory(60L, 0, invoiceCategory, "Fatura de Cartão",
                true, Instant.now(), null)]

        when:
        SystemCategoryPair pair = service.execute(1L, SystemCategory.CREDIT_CARD_INVOICE_PAYMENT)

        then:
        1 * ensureSystemCategoriesService.execute(1L)
        pair.categoryId() == 51L
        pair.subCategoryId() == 60L
    }

    def "execute returns a null subCategoryId for a system category that has no reserved subcategory"() {
        given:
        categoryRepository.findBySpaceId(1L) >> [buildCategory(52L, "Transferência")]

        when:
        SystemCategoryPair pair = service.execute(1L, SystemCategory.TRANSFER)

        then:
        pair.categoryId() == 52L
        pair.subCategoryId() == null
        0 * subCategoryRepository.findByCategoryId(_)
    }

    def "execute throws DomainException when the system category is still missing after the ensure"() {
        given:
        categoryRepository.findBySpaceId(1L) >> [buildCategory(9L, "Food")]

        when:
        service.execute(1L, SystemCategory.TRANSFER)

        then:
        thrown(DomainException)
    }
}
