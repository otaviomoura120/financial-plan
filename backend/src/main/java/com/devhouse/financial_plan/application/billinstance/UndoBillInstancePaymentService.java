package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.application.transaction.TransactionBalanceEffectService;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UndoBillInstancePaymentService {

    private final BillInstanceRepository billInstanceRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBalanceEffectService transactionBalanceEffectService;

    public UndoBillInstancePaymentService(BillInstanceRepository billInstanceRepository, TransactionRepository transactionRepository,
                                           TransactionBalanceEffectService transactionBalanceEffectService) {
        this.billInstanceRepository = billInstanceRepository;
        this.transactionRepository = transactionRepository;
        this.transactionBalanceEffectService = transactionBalanceEffectService;
    }

    @Transactional
    public void execute(Long billInstanceId) {
        BillInstance billInstance = billInstanceRepository.findById(billInstanceId);
        if (billInstance == null) {
            throw new DomainException("Bill instance not found");
        }
        if (!billInstance.isPaid()) {
            throw new DomainException("Bill instance is not paid");
        }
        Transaction transaction = transactionRepository.findById(billInstance.getPaymentTransactionId());
        transactionBalanceEffectService.revert(transaction);
        transactionRepository.delete(transaction.getId());
        billInstance.revertToPending();
        billInstanceRepository.update(billInstance);
    }
}
