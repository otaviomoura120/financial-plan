package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.enums.SystemCategory
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import spock.lang.Specification

import java.time.Instant

class EnsureSystemCategoriesServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    SpaceRepository spaceRepository = Mock()

    EnsureSystemCategoriesService service = new EnsureSystemCategoriesService(categoryRepository, subCategoryRepository,
            spaceRepository)

    private Space buildSpace() {
        new Space(1L, 0, "My Space", null, Instant.now(), null)
    }

    private Category buildCategory(Long id, String name) {
        new Category(id, 0, buildSpace(), name, true, Instant.now(), null)
    }

    def "execute creates both system categories when the space has none"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        categoryRepository.findBySpaceId(1L) >> []
        subCategoryRepository.findByCategoryId(_) >> []
        List<String> savedCategoryNames = []
        List<String> savedSubCategoryNames = []

        when:
        service.execute(1L)

        then:
        2 * categoryRepository.save(_) >> { Category category ->
            savedCategoryNames << category.getName()
            buildCategory(50L + savedCategoryNames.size(), category.getName())
        }
        1 * subCategoryRepository.save(_) >> { SubCategory subCategory ->
            savedSubCategoryNames << subCategory.getName()
            subCategory
        }
        savedCategoryNames == ["Pagamento de Fatura", "Transferência"]
        savedSubCategoryNames == ["Fatura de Cartão"]
    }

    def "execute is idempotent and creates nothing when the system categories already exist"() {
        given:
        Category invoiceCategory = buildCategory(51L, SystemCategory.CREDIT_CARD_INVOICE_PAYMENT.getCategoryName())
        Category transferCategory = buildCategory(52L, SystemCategory.TRANSFER.getCategoryName())
        categoryRepository.findBySpaceId(1L) >> [invoiceCategory, transferCategory]
        subCategoryRepository.findByCategoryId(51L) >> [new SubCategory(60L, 0, invoiceCategory,
                SystemCategory.CREDIT_CARD_INVOICE_PAYMENT.getSubCategoryName(), true, Instant.now(), null)]
        subCategoryRepository.findByCategoryId(52L) >> []

        when:
        service.execute(1L)

        then:
        0 * categoryRepository.save(_)
        0 * subCategoryRepository.save(_)
    }

    def "execute adopts a pre-existing category that already carries the reserved name instead of duplicating it"() {
        given:
        Category handMade = buildCategory(77L, "pagamento de fatura")
        categoryRepository.findBySpaceId(1L) >> [handMade]
        spaceRepository.findById(1L) >> buildSpace()
        subCategoryRepository.findByCategoryId(77L) >> []
        subCategoryRepository.findByCategoryId(_) >> []
        SubCategory savedSubCategory = null

        when:
        service.execute(1L)

        then:
        1 * categoryRepository.save({ Category category -> category.getName() == "Transferência" }) >>
                buildCategory(52L, "Transferência")
        1 * subCategoryRepository.save(_) >> { SubCategory subCategory ->
            savedSubCategory = subCategory
            subCategory
        }
        savedSubCategory.getCategory().getId() == 77L
        savedSubCategory.getName() == "Fatura de Cartão"
    }

    def "execute creates the reserved subcategory only for the credit card invoice payment category"() {
        given:
        spaceRepository.findById(1L) >> buildSpace()
        categoryRepository.findBySpaceId(1L) >> []
        subCategoryRepository.findByCategoryId(_) >> []
        List<Long> subCategoryParentIds = []

        when:
        service.execute(1L)

        then:
        2 * categoryRepository.save(_) >> { Category category ->
            buildCategory(category.getName() == "Pagamento de Fatura" ? 51L : 52L, category.getName())
        }
        1 * subCategoryRepository.save(_) >> { SubCategory subCategory ->
            subCategoryParentIds << subCategory.getCategory().getId()
            subCategory
        }
        subCategoryParentIds == [51L]
    }

    def "execute throws DomainException when the space does not exist"() {
        given:
        categoryRepository.findBySpaceId(99L) >> []
        spaceRepository.findById(99L) >> null

        when:
        service.execute(99L)

        then:
        thrown(DomainException)
        0 * categoryRepository.save(_)
    }
}
