package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.billinstance.dto.BillInstanceResponse;
import com.devhouse.financial_plan.application.billinstance.dto.PayBillInstanceRequest;
import com.devhouse.financial_plan.application.transaction.CreateTransactionService;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.TransactionSourceType;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayBillInstanceService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final CreateTransactionService createTransactionService;

    public PayBillInstanceService(BillRepository billRepository, UserRepository userRepository,
                                   CreateTransactionService createTransactionService) {
        this.billRepository = billRepository;
        this.userRepository = userRepository;
        this.createTransactionService = createTransactionService;
    }

    @Transactional
    public BillInstanceResponse execute(Long id, PayBillInstanceRequest request, String auth0Sub) {
        Bill bill = resolveBill(id);
        if (!bill.isPending()) {
            throw new DomainException("Bill instance is already paid");
        }
        User user = resolveUser(auth0Sub);
        Category category = resolveCategory(bill);
        SubCategory subCategory = bill.getSubCategory();

        CreateTransactionRequest transactionRequest = new CreateTransactionRequest(TransactionType.EXPENSE, user.getId(),
                request.bankAccountId(), null, category.getId(), subCategory != null ? subCategory.getId() : null,
                request.paymentMethodId(), bill.getAmount(), request.paidDate(), "Pagamento de conta - " + bill.getName());
        TransactionResponse paymentTransaction = createTransactionService.execute(transactionRequest,
                TransactionSourceType.BILL_INSTANCE_PAYMENT, bill.getId());

        bill.markAsPaid(request.paidDate(), paymentTransaction.id(), request.bankAccountId());
        Bill updated = billRepository.update(bill);
        return toResponse(updated);
    }

    private Bill resolveBill(Long id) {
        Bill bill = billRepository.findById(id);
        if (bill == null) {
            throw new DomainException("Bill instance not found");
        }
        return bill;
    }

    private User resolveUser(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            throw new DomainException("User not found");
        }
        return user;
    }

    private Category resolveCategory(Bill bill) {
        if (bill.getCategory() == null) {
            throw new DomainException("The bill has no category");
        }
        return bill.getCategory();
    }

    private BillInstanceResponse toResponse(Bill bill) {
        return new BillInstanceResponse(bill.getId(), bill.getVersion(),
                bill.getBillRecurring() != null ? bill.getBillRecurring().getId() : null, bill.getName(),
                bill.getCategory() != null ? bill.getCategory().getId() : null,
                bill.getSubCategory() != null ? bill.getSubCategory().getId() : null, bill.getReferenceMonth(),
                bill.getDueDate(), bill.getAmount(), bill.getStatus(), bill.getPaidDate(), bill.getPaymentTransactionId(),
                bill.getBankAccountId(), bill.getCreatedDate());
    }
}
