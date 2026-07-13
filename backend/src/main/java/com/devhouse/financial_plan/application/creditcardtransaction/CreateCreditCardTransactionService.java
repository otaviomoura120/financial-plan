package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreateCreditCardTransactionRequest;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CreateCreditCardTransactionService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public CreateCreditCardTransactionService(CreditCardTransactionRepository creditCardTransactionRepository,
                                               CreditCardRepository creditCardRepository, UserRepository userRepository,
                                               CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                               CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public CreditCardTransactionResponse execute(CreateCreditCardTransactionRequest request) {
        CreditCard creditCard = resolveCreditCard(request.creditCardId());
        User user = resolveUser(request.userId());
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());

        int totalInstallments = request.totalInstallments() == null || request.totalInstallments() <= 1
                ? 1 : request.totalInstallments();
        LocalDate firstReferenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(request.purchaseDate(), creditCard.getClosingDay());
        List<LocalDate> referenceMonths = new ArrayList<>();
        for (int i = 0; i < totalInstallments; i++) {
            referenceMonths.add(firstReferenceMonth.plusMonths(i));
        }
        rejectIfAnyMonthAlreadyPaid(creditCard.getId(), referenceMonths);

        List<BigDecimal> installmentAmounts = splitAmount(request.amount(), totalInstallments);
        String installmentGroupId = UUID.randomUUID().toString();

        CreditCardTransaction firstInstallment = null;
        for (int i = 0; i < totalInstallments; i++) {
            CreditCardTransaction installment = new CreditCardTransaction(null, 0, creditCard, user, category, subCategory,
                    installmentAmounts.get(i), request.purchaseDate(), request.description(), referenceMonths.get(i),
                    installmentGroupId, i + 1, totalInstallments, false, null, Instant.now(), null);
            installment.validate();
            CreditCardTransaction saved = creditCardTransactionRepository.save(installment);
            if (i == 0) {
                firstInstallment = saved;
            }
        }
        return toResponse(firstInstallment, request.amount());
    }

    private List<BigDecimal> splitAmount(BigDecimal totalAmount, int totalInstallments) {
        List<BigDecimal> amounts = new ArrayList<>();
        if (totalInstallments == 1) {
            amounts.add(totalAmount);
            return amounts;
        }
        BigDecimal base = totalAmount.divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.DOWN);
        BigDecimal sumOfFirstInstallments = base.multiply(BigDecimal.valueOf(totalInstallments - 1));
        BigDecimal lastInstallment = totalAmount.subtract(sumOfFirstInstallments);
        for (int i = 0; i < totalInstallments - 1; i++) {
            amounts.add(base);
        }
        amounts.add(lastInstallment);
        return amounts;
    }

    private void rejectIfAnyMonthAlreadyPaid(Long creditCardId, List<LocalDate> referenceMonths) {
        for (LocalDate referenceMonth : referenceMonths) {
            if (creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(creditCardId, referenceMonth) != null) {
                throw new DomainException("Cannot create a transaction in an invoice that is already paid");
            }
        }
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

    private CreditCardTransactionResponse toResponse(CreditCardTransaction t, BigDecimal totalAmount) {
        return new CreditCardTransactionResponse(t.getId(), t.getVersion(), t.getCreditCard().getId(), t.getUser().getId(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(), t.getPurchaseDate(),
                t.getDescription(), t.getReferenceMonth(), t.getInstallmentGroupId(), t.getInstallmentNumber(),
                t.getTotalInstallments(), t.isAnticipated(), t.getOriginalReferenceMonth(), t.getCreatedDate(), totalAmount);
    }
}
