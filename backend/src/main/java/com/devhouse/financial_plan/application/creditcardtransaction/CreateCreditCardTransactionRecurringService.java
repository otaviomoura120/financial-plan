package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRecurringRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.CategoryKindPolicy;
import com.devhouse.financial_plan.domain.SystemCategoryPolicy;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreateCreditCardTransactionRecurringService {

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService;

    public CreateCreditCardTransactionRecurringService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                         CreditCardRepository creditCardRepository, UserRepository userRepository,
                                                         CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                                         EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.ensureRecurringCreditCardTransactionsGeneratedService = ensureRecurringCreditCardTransactionsGeneratedService;
    }

    @Transactional
    public CreditCardTransactionRecurringResponse execute(Long spaceId, String auth0Sub,
                                                          CreateCreditCardTransactionRecurringRequest request) {
        CreditCard creditCard = resolveCreditCard(request.creditCardId(), spaceId);
        User user = resolveUser(auth0Sub);
        Category category = resolveCategory(request.categoryId(), spaceId);
        SubCategory subCategory = resolveSubCategory(request.subCategoryId(), spaceId);
        SystemCategoryPolicy.rejectSystemSelection(category, subCategory);
        CategoryKindPolicy.requireExpense(category);

        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(null, 0, creditCard, user, category,
                subCategory, request.description(), request.defaultAmount(), request.startDate(), true, Instant.now(), null);
        recurring.validate();
        CreditCardTransactionRecurring saved = creditCardTransactionRecurringRepository.save(recurring);
        ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(saved);
        return toResponse(saved);
    }

    private CreditCard resolveCreditCard(Long creditCardId, Long spaceId) {
        CreditCard creditCard = creditCardId != null ? creditCardRepository.findById(creditCardId) : null;
        if (creditCard == null || !creditCard.belongsToSpace(spaceId)) {
            throw new DomainException("Credit card not found");
        }
        return creditCard;
    }

    private User resolveUser(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private Category resolveCategory(Long categoryId, Long spaceId) {
        Category category = categoryId != null ? categoryRepository.findById(categoryId) : null;
        if (category == null || !category.belongsToSpace(spaceId)) {
            throw new DomainException("Category not found");
        }
        return category;
    }

    private SubCategory resolveSubCategory(Long subCategoryId, Long spaceId) {
        if (subCategoryId == null) {
            return null;
        }
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId);
        if (subCategory == null || !subCategory.belongsToSpace(spaceId)) {
            throw new DomainException("Sub category not found");
        }
        return subCategory;
    }

    private CreditCardTransactionRecurringResponse toResponse(CreditCardTransactionRecurring recurring) {
        return new CreditCardTransactionRecurringResponse(recurring.getId(), recurring.getVersion(), recurring.getCreditCard().getId(),
                recurring.getUser().getId(), recurring.getCategory() != null ? recurring.getCategory().getId() : null,
                recurring.getSubCategory() != null ? recurring.getSubCategory().getId() : null, recurring.getDescription(),
                recurring.getDefaultAmount(), recurring.getStartDate(), recurring.isActive(), recurring.getCreatedDate());
    }
}
