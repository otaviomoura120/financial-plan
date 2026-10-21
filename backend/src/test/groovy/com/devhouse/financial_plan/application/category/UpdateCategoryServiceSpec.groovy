package com.devhouse.financial_plan.application.category

import com.devhouse.financial_plan.application.category.dto.CategoryResponse
import com.devhouse.financial_plan.domain.enums.CategoryKind
import com.devhouse.financial_plan.application.category.dto.UpdateCategoryRequest
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.enums.TransactionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository
import com.devhouse.financial_plan.domain.repository.BillRepository
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.TransactionRepository
import spock.lang.Specification

import java.time.Instant

class UpdateCategoryServiceSpec extends Specification {

    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()
    CategoryNameValidator categoryNameValidator = new CategoryNameValidator(categoryRepository, subCategoryRepository)
    TransactionRepository transactionRepository = Mock()
    BillRepository billRepository = Mock()
    BillRecurringRepository billRecurringRepository = Mock()
    CreditCardTransactionRepository creditCardTransactionRepository = Mock()
    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CategoryKindChangeGuard categoryKindChangeGuard = new CategoryKindChangeGuard(transactionRepository, billRepository,
            billRecurringRepository, creditCardTransactionRepository, creditCardTransactionRecurringRepository)
    UpdateCategoryService service = new UpdateCategoryService(categoryRepository, categoryNameValidator,
            categoryKindChangeGuard)

    private Category buildCategory() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new Category(10L, 0, space, "Food", CategoryKind.EXPENSE, true, Instant.now(), null)
    }

    def "execute renames the category"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        categoryRepository.update(_) >> { Category c -> c }
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Groceries", CategoryKind.EXPENSE)

        when:
        CategoryResponse response = service.execute(1L, 10L, request)

        then:
        response.name() == "Groceries"
        response.subCategories().isEmpty()
    }

    def "execute throws DomainException when the new name is blank"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "", CategoryKind.EXPENSE)

        when:
        service.execute(1L, 10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute throws ObjectOptimisticLockingFailureException when the version does not match"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        UpdateCategoryRequest request = new UpdateCategoryRequest(99, "Groceries", CategoryKind.EXPENSE)

        when:
        service.execute(1L, 10L, request)

        then:
        thrown(org.springframework.orm.ObjectOptimisticLockingFailureException)
        0 * categoryRepository.update(_)
    }


    def "execute allows renaming a category to its own current name"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        categoryRepository.update(_) >> { Category c -> c }
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Food", CategoryKind.EXPENSE)

        when:
        CategoryResponse response = service.execute(1L, 10L, request)

        then:
        response.name() == "Food"
    }

    def "execute throws DomainException when another category already uses the name, ignoring case"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category, new Category(11L, 0, space, "Transport", CategoryKind.EXPENSE, true, Instant.now(), null)]
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "TRANSPORT", CategoryKind.EXPENSE)

        when:
        service.execute(1L, 10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute throws DomainException when the category is a system category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        categoryRepository.findById(10L) >> new Category(10L, 0, space, "Pagamento de Fatura", CategoryKind.EXPENSE, true, Instant.now(), null)
        UpdateCategoryRequest request = new UpdateCategoryRequest(0, "Outra coisa", CategoryKind.EXPENSE)

        when:
        service.execute(1L, 10L, request)

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute keeps the type without checking usages when it does not change"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        categoryRepository.update(_) >> { Category c -> c }

        when:
        CategoryResponse response = service.execute(1L, 10L, new UpdateCategoryRequest(0, "Food", CategoryKind.EXPENSE))

        then:
        response.kind() == CategoryKind.EXPENSE
        0 * transactionRepository.countByCategoryIdAndType(_, _)
        0 * billRepository.countByCategoryId(_)
    }

    def "execute changes the type when nothing incompatible uses the category"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        transactionRepository.countByCategoryIdAndType(10L, TransactionType.EXPENSE) >> 0L

        when:
        CategoryResponse response = service.execute(1L, 10L, new UpdateCategoryRequest(0, "Food", CategoryKind.INCOME))

        then:
        1 * categoryRepository.update({ Category c -> c.getKind() == CategoryKind.INCOME }) >> { Category c -> c }
        response.kind() == CategoryKind.INCOME
    }

    def "execute blocks switching to income while expense entries use the category"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        transactionRepository.countByCategoryIdAndType(10L, TransactionType.EXPENSE) >> 50L
        billRepository.countByCategoryId(10L) >> 3L
        billRecurringRepository.countByCategoryId(10L) >> 0L
        creditCardTransactionRepository.countByCategoryIdAndCredit(10L, false) >> 2L
        creditCardTransactionRecurringRepository.countByCategoryId(10L) >> 1L

        when:
        service.execute(1L, 10L, new UpdateCategoryRequest(0, "Food", CategoryKind.INCOME))

        then:
        DomainException exception = thrown()
        exception.message == "Cannot change the category type: it is used by 50 expense transaction(s), 3 bill(s), " +
                "2 credit card transaction(s), 1 credit card subscription(s). Move them to another category first."
        category.getKind() == CategoryKind.EXPENSE
        0 * categoryRepository.update(_)
    }

    def "execute blocks switching to expense while income transactions use the category"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        Category category = new Category(10L, 0, space, "Salário", CategoryKind.INCOME, true, Instant.now(), null)
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]
        transactionRepository.countByCategoryIdAndType(10L, TransactionType.INCOME) >> 12L
        creditCardTransactionRepository.countByCategoryIdAndCredit(10L, true) >> 1L

        when:
        service.execute(1L, 10L, new UpdateCategoryRequest(0, "Salário", CategoryKind.EXPENSE))

        then:
        DomainException exception = thrown()
        exception.message == "Cannot change the category type: it is used by 12 income transaction(s), " +
                "1 credit card invoice credit(s). Move them to another category first."
        0 * billRepository.countByCategoryId(_)
        0 * categoryRepository.update(_)
    }

    def "execute throws DomainException when the type is missing"() {
        given:
        Category category = buildCategory()
        categoryRepository.findById(10L) >> category
        categoryRepository.findBySpaceId(1L) >> [category]

        when:
        service.execute(1L, 10L, new UpdateCategoryRequest(0, "Food", null))

        then:
        thrown(DomainException)
        0 * categoryRepository.update(_)
    }

    def "execute rejects a category that belongs to another space"() {
        given:
        categoryRepository.findById(10L) >> buildCategory()

        when:
        service.execute(2L, 10L, new UpdateCategoryRequest(0, "Groceries", CategoryKind.EXPENSE))

        then:
        DomainException exception = thrown()
        exception.message == "Category not found"
        0 * categoryRepository.update(_)
    }
}
