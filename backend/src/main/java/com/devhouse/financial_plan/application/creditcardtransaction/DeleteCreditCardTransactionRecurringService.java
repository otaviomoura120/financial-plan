package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class DeleteCreditCardTransactionRecurringService {

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public DeleteCreditCardTransactionRecurringService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                        CreditCardTransactionRepository creditCardTransactionRepository,
                                                        CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public void execute(Long id) {
        CreditCardTransactionRecurring recurring = creditCardTransactionRecurringRepository.findById(id);
        if (recurring == null) {
            throw new DomainException("Credit card transaction recurring not found");
        }
        removeFutureAndDetachPastTransactions(id);
        creditCardTransactionRecurringRepository.delete(id);
    }

    private void removeFutureAndDetachPastTransactions(Long recurringId) {
        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (CreditCardTransaction transaction : creditCardTransactionRepository.findByCreditCardTransactionRecurringId(recurringId)) {
            if (isFromCurrentMonthOrLater(transaction, currentMonth) && !isInvoiceAlreadyPaid(transaction)) {
                creditCardTransactionRepository.delete(transaction.getId());
            }
            else {
                transaction.detachFromRecurring();
                creditCardTransactionRepository.update(transaction);
            }
        }
    }

    private boolean isFromCurrentMonthOrLater(CreditCardTransaction transaction, LocalDate currentMonth) {
        return !transaction.getReferenceMonth().isBefore(currentMonth);
    }

    private boolean isInvoiceAlreadyPaid(CreditCardTransaction transaction) {
        return creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                transaction.getCreditCard().getId(), transaction.getReferenceMonth()) != null;
    }
}
