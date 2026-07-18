package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.transaction.TransactionBalanceEffectService;
import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UndoBillInstancePaymentService {

    private final BillRepository billRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBalanceEffectService transactionBalanceEffectService;

    public UndoBillInstancePaymentService(BillRepository billRepository, TransactionRepository transactionRepository,
                                           TransactionBalanceEffectService transactionBalanceEffectService) {
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
        this.transactionBalanceEffectService = transactionBalanceEffectService;
    }

    @Transactional
    public void execute(Long billId) {
        Bill bill = billRepository.findById(billId);
        if (bill == null) {
            throw new DomainException("Bill instance not found");
        }
        if (!bill.isPaid()) {
            throw new DomainException("Bill instance is not paid");
        }
        Transaction transaction = transactionRepository.findById(bill.getPaymentTransactionId());
        transactionBalanceEffectService.revert(transaction);
        transactionRepository.delete(transaction.getId());
        bill.revertToPending();
        billRepository.update(bill);
    }
}
