package com.devhouse.financial_plan.application.creditcardtransaction

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRecurringRequest
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse
import com.devhouse.financial_plan.domain.Category
import com.devhouse.financial_plan.domain.CreditCard
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SubCategory
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.CategoryRepository
import com.devhouse.financial_plan.domain.repository.CreditCardRepository
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDate

class CreateCreditCardTransactionRecurringServiceSpec extends Specification {

    CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository = Mock()
    CreditCardRepository creditCardRepository = Mock()
    UserRepository userRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    SubCategoryRepository subCategoryRepository = Mock()

    CreateCreditCardTransactionRecurringService service = new CreateCreditCardTransactionRecurringService(
            creditCardTransactionRecurringRepository, creditCardRepository, userRepository, categoryRepository, subCategoryRepository)

    private CreditCard buildCreditCard() {
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        new CreditCard(20L, 0, space, null, "Nubank", new BigDecimal("5000.00"), 10, 17, true, Instant.now(), null)
    }

    private User buildUser() {
        new User(1L, 0, "auth0|1", "User 1", null, null, null, null, "user1@test.com", null, true,
                null, null, Instant.now(), null, false)
    }

    def "execute creates a credit card transaction recurring"() {
        given:
        creditCardRepository.findById(20L) >> buildCreditCard()
        userRepository.findById(1L) >> buildUser()
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        categoryRepository.findById(30L) >> category
        creditCardTransactionRecurringRepository.save(_) >> { CreditCardTransactionRecurring r ->
            new CreditCardTransactionRecurring(10L, 0, r.creditCard, r.user, r.category, r.subCategory, r.description,
                    r.defaultAmount, r.startDate, r.active, r.createdDate, r.updatedDate)
        }
        CreateCreditCardTransactionRecurringRequest request = new CreateCreditCardTransactionRecurringRequest(20L, 1L, 30L,
                null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10))

        when:
        CreditCardTransactionRecurringResponse response = service.execute(request)

        then:
        response.id() == 10L
        response.creditCardId() == 20L
        response.categoryId() == 30L
        response.description() == "Netflix"
        response.defaultAmount() == new BigDecimal("39.90")
        response.startDate() == LocalDate.of(2026, 3, 10)
        response.active()
    }

    def "execute resolves the optional subCategory when informed"() {
        given:
        creditCardRepository.findById(20L) >> buildCreditCard()
        userRepository.findById(1L) >> buildUser()
        Category category = new Category(30L, 0, null, "Assinaturas", true, Instant.now(), null)
        categoryRepository.findById(30L) >> category
        SubCategory subCategory = new SubCategory(40L, 0, category, "Streaming", true, null, null)
        subCategoryRepository.findById(40L) >> subCategory
        creditCardTransactionRecurringRepository.save(_) >> { CreditCardTransactionRecurring r ->
            new CreditCardTransactionRecurring(10L, 0, r.creditCard, r.user, r.category, r.subCategory, r.description,
                    r.defaultAmount, r.startDate, r.active, r.createdDate, r.updatedDate)
        }
        CreateCreditCardTransactionRecurringRequest request = new CreateCreditCardTransactionRecurringRequest(20L, 1L, 30L,
                40L, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10))

        when:
        CreditCardTransactionRecurringResponse response = service.execute(request)

        then:
        response.subCategoryId() == 40L
    }

    def "execute throws DomainException when credit card does not exist"() {
        given:
        creditCardRepository.findById(20L) >> null
        CreateCreditCardTransactionRecurringRequest request = new CreateCreditCardTransactionRecurringRequest(20L, 1L, 30L,
                null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRecurringRepository.save(_)
    }

    def "execute throws DomainException when category does not exist"() {
        given:
        creditCardRepository.findById(20L) >> buildCreditCard()
        userRepository.findById(1L) >> buildUser()
        categoryRepository.findById(30L) >> null
        CreateCreditCardTransactionRecurringRequest request = new CreateCreditCardTransactionRecurringRequest(20L, 1L, 30L,
                null, "Netflix", new BigDecimal("39.90"), LocalDate.of(2026, 3, 10))

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * creditCardTransactionRecurringRepository.save(_)
    }
}
