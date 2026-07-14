package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionResponse;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRequest;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UpdateCreditCardTransactionService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public UpdateCreditCardTransactionService(CreditCardTransactionRepository creditCardTransactionRepository,
                                               CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                               CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public CreditCardTransactionResponse execute(Long id, UpdateCreditCardTransactionRequest request) {
        CreditCardTransaction transaction = creditCardTransactionRepository.findById(id);
        if (transaction == null) {
            throw new DomainException("Credit card transaction not found");
        }
        rejectIfMonthAlreadyPaid(transaction);
        transaction.setVersion(request.version());

        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());

        transaction.update(category, subCategory, request.amount(), request.purchaseDate(), request.description());
        transaction.validate();
        CreditCardTransaction updated = creditCardTransactionRepository.update(transaction);
        return toResponse(updated, resolveTotalAmount(updated));
    }

    private BigDecimal resolveTotalAmount(CreditCardTransaction transaction) {
        if (transaction.getTotalInstallments() == null || transaction.getTotalInstallments() <= 1) {
            return transaction.getAmount();
        }
        return creditCardTransactionRepository.findByInstallmentGroupId(transaction.getInstallmentGroupId()).stream()
                .map(CreditCardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void rejectIfMonthAlreadyPaid(CreditCardTransaction transaction) {
        boolean alreadyPaid = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                transaction.getCreditCard().getId(), transaction.getReferenceMonth()) != null;
        if (alreadyPaid) {
            throw new DomainException("Cannot modify a transaction from a paid invoice");
        }
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
                t.getDescription(), t.getReferenceMonth(), t.getCompetenceMonth(), t.getInstallmentGroupId(), t.getInstallmentNumber(),
                t.getTotalInstallments(), t.isAnticipated(), t.getOriginalReferenceMonth(), t.getCreatedDate(), totalAmount);
    }
}
