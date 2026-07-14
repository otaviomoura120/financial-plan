package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRecurringRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.SubCategory;
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

    public CreateCreditCardTransactionRecurringService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                         CreditCardRepository creditCardRepository, UserRepository userRepository,
                                                         CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    public CreditCardTransactionRecurringResponse execute(CreateCreditCardTransactionRecurringRequest request) {
        CreditCard creditCard = resolveCreditCard(request.creditCardId());
        User user = resolveUser(request.userId());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());

        CreditCardTransactionRecurring recurring = new CreditCardTransactionRecurring(null, 0, creditCard, user, category,
                subCategory, request.description(), request.defaultAmount(), request.startDate(), true, Instant.now(), null);
        recurring.validate();
        CreditCardTransactionRecurring saved = creditCardTransactionRecurringRepository.save(recurring);
        return toResponse(saved);
    }

    private CreditCard resolveCreditCard(Long creditCardId) {
        CreditCard creditCard = creditCardId != null ? creditCardRepository.findById(creditCardId) : null;
        if (creditCard == null) {
            throw new DomainException("Credit card not found");
        }
        return creditCard;
    }

    private User resolveUser(Long userId) {
        User user = userId != null ? userRepository.findById(userId) : null;
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private Category resolveCategory(Long categoryId) {
        Category category = categoryId != null ? categoryRepository.findById(categoryId) : null;
        if (category == null) {
            throw new DomainException("Category not found");
        }
        return category;
    }

    private SubCategory resolveSubCategory(Long subCategoryId) {
        if (subCategoryId == null) {
            return null;
        }
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId);
        if (subCategory == null) {
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
