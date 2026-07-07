package com.devhouse.financial_plan.application.paymentmethod;

import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class DeletePaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;

    public DeletePaymentMethodService(PaymentMethodRepository paymentMethodRepository, TransactionRepository transactionRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Long id) {
        if (transactionRepository.existsByPaymentMethodId(id)) {
            throw new DomainException("Cannot delete payment method: there are transactions linked to it.");
        }
        paymentMethodRepository.delete(id);
    }
}
