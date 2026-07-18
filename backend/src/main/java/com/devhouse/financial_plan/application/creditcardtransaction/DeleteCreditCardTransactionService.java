package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeleteCreditCardTransactionService {

    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public DeleteCreditCardTransactionService(CreditCardTransactionRepository creditCardTransactionRepository,
                                               CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public void execute(Long id, boolean includeFuture) {
        CreditCardTransaction transaction = creditCardTransactionRepository.findById(id);
        if (transaction == null) {
            throw new DomainException("Credit card transaction not found");
        }

        List<CreditCardTransaction> toDelete = resolveTransactionsToDelete(transaction, includeFuture);
        for (CreditCardTransaction candidate : toDelete) {
            rejectIfMonthAlreadyPaid(candidate);
        }
        for (CreditCardTransaction candidate : toDelete) {
            creditCardTransactionRepository.delete(candidate.getId());
        }
    }

    private List<CreditCardTransaction> resolveTransactionsToDelete(CreditCardTransaction transaction, boolean includeFuture) {
        if (!includeFuture || transaction.getTotalInstallments() == null || transaction.getTotalInstallments() <= 1) {
            return List.of(transaction);
        }
        return creditCardTransactionRepository.findByInstallmentGroupId(transaction.getInstallmentGroupId()).stream()
                .filter(candidate -> candidate.getInstallmentNumber() >= transaction.getInstallmentNumber())
                .toList();
    }

    private void rejectIfMonthAlreadyPaid(CreditCardTransaction transaction) {
        boolean alreadyPaid = creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                transaction.getCreditCard().getId(), transaction.getReferenceMonth()) != null;
        if (alreadyPaid) {
            throw new DomainException("Cannot delete a transaction from a paid invoice");
        }
    }
}
