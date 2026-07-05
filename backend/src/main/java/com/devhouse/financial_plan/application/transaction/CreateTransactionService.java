package com.devhouse.financial_plan.application.transaction;

import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import com.devhouse.financial_plan.domain.repository.SubCategoryRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateTransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    public CreateTransactionService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository,
                                     CategoryRepository categoryRepository, SubCategoryRepository subCategoryRepository,
                                     PaymentMethodRepository paymentMethodRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userRepository = userRepository;
    }

    public TransactionResponse execute(CreateTransactionRequest request) {
        validateForeignKeys(request);
        Transaction transaction = new Transaction(null, 0, request.type(), request.userId(),
                request.bankAccountId(), request.destinationBankAccountId(), request.categoryId(), request.subCategoryId(),
                request.paymentMethodId(), request.amount(), request.transactionDate(),
                request.description(), Instant.now(), null);
        transaction.validate();
        Transaction saved = transactionRepository.save(transaction);
        return toResponse(saved);
    }

    private void validateForeignKeys(CreateTransactionRequest request) {
        if (userRepository.findById(request.userId()) == null) {
            throw new DomainException("User not found");
        }
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
