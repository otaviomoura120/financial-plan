package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionBalanceEffectService balanceEffectService;

    public UpdateTransactionService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                     CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                     PaymentMethodRepository paymentMethodRepository,
                                     TransactionBalanceEffectService balanceEffectService) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.balanceEffectService = balanceEffectService;
    }

    @Transactional
    public TransactionResponse execute(Long id, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id);
        transaction.setVersion(request.version());
        Transaction old = snapshot(transaction);

        validateForeignKeys(request);
        balanceEffectService.revert(old);

        transaction.update(request.type(), request.bankAccountId(), request.destinationBankAccountId(), request.categoryId(),
                request.subCategoryId(), request.paymentMethodId(), request.amount(),
                request.transactionDate(), request.description());
        transaction.validate();
        balanceEffectService.apply(transaction);

        Transaction updated = transactionRepository.update(transaction);
        return toResponse(updated);
    }

    private Transaction snapshot(Transaction transaction) {
        return new Transaction(transaction.getId(), transaction.getVersion(), transaction.getType(), transaction.getUserId(),
                transaction.getBankAccountId(), transaction.getDestinationBankAccountId(), transaction.getCategoryId(),
                transaction.getSubCategoryId(), transaction.getPaymentMethodId(), transaction.getAmount(),
                transaction.getTransactionDate(), transaction.getDescription(), transaction.getCreatedDate(),
                transaction.getUpdatedDate());
    }

    private void validateForeignKeys(UpdateTransactionRequest request) {
        if (bankAccountRepository.findById(request.bankAccountId()) == null) {
            throw new DomainException("Bank account not found");
        }
        if (TransactionType.TRANSFER.equals(request.type())) {
            validateDestinationBankAccount(request.destinationBankAccountId());
        } else {
            validateCategoryAndPaymentMethod(request.categoryId(), request.paymentMethodId());
        }
        if (request.subCategoryId() != null && subCategoryRepository.findById(request.subCategoryId()) == null) {
            throw new DomainException("Sub category not found");
        }
    }

    private void validateDestinationBankAccount(Long destinationBankAccountId) {
        if (destinationBankAccountId != null && bankAccountRepository.findById(destinationBankAccountId) == null) {
            throw new DomainException("Destination bank account not found");
        }
    }

    private void validateCategoryAndPaymentMethod(Long categoryId, Long paymentMethodId) {
        if (categoryId != null && categoryRepository.findById(categoryId) == null) {
            throw new DomainException("Category not found");
        }
        if (paymentMethodId != null && paymentMethodRepository.findById(paymentMethodId) == null) {
            throw new DomainException("Payment method not found");
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getVersion(), t.getType(), t.getUserId(), t.getBankAccountId(),
                t.getDestinationBankAccountId(), t.getCategoryId(), t.getSubCategoryId(), t.getPaymentMethodId(), t.getAmount(),
                t.getTransactionDate(), t.getDescription(), t.getCreatedDate());
    }
}
