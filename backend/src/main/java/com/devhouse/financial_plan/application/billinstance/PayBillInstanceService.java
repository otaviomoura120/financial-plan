package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest;
import com.devhouse.financial_plan.application.transaction.CreateTransactionService;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import com.devhouse.financial_plan.domain.repository.CategoryRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayBillInstanceService {

    private final BillInstanceRepository billInstanceRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CreateTransactionService createTransactionService;

    public PayBillInstanceService(BillInstanceRepository billInstanceRepository, CategoryRepository categoryRepository,
                                   UserRepository userRepository, CreateTransactionService createTransactionService) {
        this.billInstanceRepository = billInstanceRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.createTransactionService = createTransactionService;
    }

    @Transactional
    public BillInstanceResponse execute(Long id, PayBillInstanceRequest request, String auth0Sub) {
        BillInstance instance = resolveInstance(id);
        if (!instance.isPending()) {
            throw new DomainException("Bill instance is already paid");
        }
        Bill bill = instance.getBill();
        User user = resolveUser(auth0Sub);
        Category category = resolveCategory(request.categoryId(), bill);

        CreateTransactionRequest transactionRequest = new CreateTransactionRequest(TransactionType.EXPENSE, user.getId(),
                request.bankAccountId(), null, category.getId(), null, request.paymentMethodId(), instance.getAmount(),
                request.paidDate(), "Pagamento de conta - " + bill.getName());
        TransactionResponse paymentTransaction = createTransactionService.execute(transactionRequest,
                TransactionSourceType.BILL_INSTANCE_PAYMENT, bill.getId());

        instance.markAsPaid(request.paidDate(), paymentTransaction.id(), request.bankAccountId());
        BillInstance updated = billInstanceRepository.update(instance);
        return toResponse(updated);
    }

    private BillInstance resolveInstance(Long id) {
        BillInstance instance = billInstanceRepository.findById(id);
        if (instance == null) {
            throw new DomainException("Bill instance not found");
        }
        return instance;
    }

    private User resolveUser(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private Category resolveCategory(Long categoryId, Bill bill) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId);
            if (category == null) {
                throw new DomainException("Category not found");
            }
            return category;
        }
        if (bill.getCategory() == null) {
            throw new DomainException("No category informed and the bill has no default category");
        }
        return bill.getCategory();
    }

    private BillInstanceResponse toResponse(BillInstance instance) {
        return new BillInstanceResponse(instance.getId(), instance.getVersion(), instance.getBill().getId(),
                instance.getBill().getName(), instance.getReferenceMonth(), instance.getDueDate(), instance.getAmount(),
                instance.getStatus(), instance.getPaidDate(), instance.getPaymentTransactionId(), instance.getBankAccountId(),
                instance.getCreatedDate());
    }
}
