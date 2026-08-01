package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.category.ResolveSystemCategoryService;
import com.devhouse.financial_plan.application.category.dto.SystemCategoryPair;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.SystemCategoryPolicy;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.SystemCategory;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreateTransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;
    private final TransactionBalanceEffectService balanceEffectService;
    private final ResolveSystemCategoryService resolveSystemCategoryService;

    public CreateTransactionService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                     CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                     UserRepository userRepository,
                                     TransactionBalanceEffectService balanceEffectService,
                                     ResolveSystemCategoryService resolveSystemCategoryService) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.userRepository = userRepository;
        this.balanceEffectService = balanceEffectService;
        this.resolveSystemCategoryService = resolveSystemCategoryService;
    }

    @Transactional
    public TransactionResponse execute(CreateTransactionRequest request) {
        return execute(request, null, null);
    }

    @Transactional
    public TransactionResponse execute(CreateTransactionRequest request, TransactionSourceType sourceType, Long sourceId) {
        User user = resolveUser(request.userId());
        BankAccount bankAccount = resolveBankAccount(request.bankAccountId(), "Bank account not found");
        BankAccount destinationBankAccount = null;
        if (TransactionType.TRANSFER.equals(request.type())) {
            destinationBankAccount = resolveBankAccount(request.destinationBankAccountId(), "Destination bank account not found");
        }
        CategorySelection selection = resolveCategorySelection(request, bankAccount, sourceType);

        Transaction transaction = new Transaction(null, 0, request.type(), user, bankAccount, destinationBankAccount,
                selection.category(), selection.subCategory(), request.amount(), request.transactionDate(),
                request.description(), Instant.now(), null, sourceType, sourceId);
        transaction.validate();
        balanceEffectService.apply(transaction);
        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    private CategorySelection resolveCategorySelection(CreateTransactionRequest request, BankAccount bankAccount,
                                                        TransactionSourceType sourceType) {
        if (TransactionType.TRANSFER.equals(request.type())) {
            return resolveTransferCategorySelection(bankAccount);
        }
        Category category = resolveCategory(request.categoryId());
        SubCategory subCategory = resolveSubCategory(request.subCategoryId());
        SystemCategoryPolicy.rejectManualSelection(category, subCategory, sourceType);
        return new CategorySelection(category, subCategory);
    }

    private CategorySelection resolveTransferCategorySelection(BankAccount bankAccount) {
        SystemCategoryPair pair = resolveSystemCategoryService.execute(bankAccount.getSpace().getId(),
                SystemCategory.TRANSFER);
        return new CategorySelection(resolveCategory(pair.categoryId()), resolveSubCategory(pair.subCategoryId()));
    }

    private record CategorySelection(Category category, SubCategory subCategory) {}

    private User resolveUser(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private BankAccount resolveBankAccount(Long bankAccountId, String errorMessage) {
        BankAccount bankAccount = bankAccountId != null ? bankAccountRepository.findById(bankAccountId) : null;
        if (bankAccount == null) {
            throw new DomainException(errorMessage);
        }
        return bankAccount;
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

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUser().getId(), t.getBankAccount().getId(),
                t.getDestinationBankAccount() != null ? t.getDestinationBankAccount().getId() : null,
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getSubCategory() != null ? t.getSubCategory().getId() : null, t.getAmount(),
                t.getTransactionDate(), t.getDescription(), t.getCreatedDate(), t.getSourceType(), t.getSourceId(), null);
    }
}
