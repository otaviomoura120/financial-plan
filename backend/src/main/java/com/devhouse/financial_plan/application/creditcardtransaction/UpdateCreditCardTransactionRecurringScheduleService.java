package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.application.creditcardtransaction.dto.CreditCardTransactionRecurringResponse;
import com.devhouse.financial_plan.application.creditcardtransaction.dto.UpdateCreditCardTransactionRecurringScheduleRequest;
import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
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
public class UpdateCreditCardTransactionRecurringScheduleService {

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public UpdateCreditCardTransactionRecurringScheduleService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                                CreditCardTransactionRepository creditCardTransactionRepository,
                                                                CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public CreditCardTransactionRecurringResponse execute(Long id, UpdateCreditCardTransactionRecurringScheduleRequest request) {
        CreditCardTransactionRecurring recurring = creditCardTransactionRecurringRepository.findById(id);
        if (recurring == null) {
            throw new DomainException("Credit card transaction recurring not found");
        }
        recurring.setVersion(request.version());
        recurring.updateSchedule(request.startDate());
        recurring.validate();
        CreditCardTransactionRecurring updated = creditCardTransactionRecurringRepository.update(recurring);
        updateCurrentAndFutureTransactionDates(updated);
        return toResponse(updated);
    }

    private void updateCurrentAndFutureTransactionDates(CreditCardTransactionRecurring recurring) {
        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (CreditCardTransaction transaction : creditCardTransactionRepository.findByCreditCardTransactionRecurringId(recurring.getId())) {
            if (!isFromCurrentMonthOrLater(transaction, currentMonth) || isInvoiceAlreadyPaid(transaction, transaction.getReferenceMonth())) {
                continue;
            }
            LocalDate newPurchaseDate = resolvePurchaseDate(recurring, transaction.getPurchaseDate());
            LocalDate newReferenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(newPurchaseDate, recurring.getCreditCard().getClosingDay());
            if (isInvoiceAlreadyPaid(transaction, newReferenceMonth)) {
                continue;
            }
            transaction.update(transaction.getCategory(), transaction.getSubCategory(), transaction.getAmount(), newPurchaseDate,
                    transaction.getDescription());
            transaction.setReferenceMonth(newReferenceMonth);
            creditCardTransactionRepository.update(transaction);
        }
    }

    private boolean isFromCurrentMonthOrLater(CreditCardTransaction transaction, LocalDate currentMonth) {
        return !transaction.getReferenceMonth().isBefore(currentMonth);
    }

    private boolean isInvoiceAlreadyPaid(CreditCardTransaction transaction, LocalDate referenceMonth) {
        return creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(
                transaction.getCreditCard().getId(), referenceMonth) != null;
    }

    private LocalDate resolvePurchaseDate(CreditCardTransactionRecurring recurring, LocalDate currentPurchaseDate) {
        YearMonth purchaseMonth = YearMonth.from(currentPurchaseDate);
        int dayOfMonth = Math.min(recurring.getStartDate().getDayOfMonth(), purchaseMonth.lengthOfMonth());
        return purchaseMonth.atDay(dayOfMonth);
    }

    private CreditCardTransactionRecurringResponse toResponse(CreditCardTransactionRecurring recurring) {
        return new CreditCardTransactionRecurringResponse(recurring.getId(), recurring.getVersion(), recurring.getCreditCard().getId(),
                recurring.getUser().getId(), recurring.getCategory() != null ? recurring.getCategory().getId() : null,
                recurring.getSubCategory() != null ? recurring.getSubCategory().getId() : null, recurring.getDescription(),
                recurring.getDefaultAmount(), recurring.getStartDate(), recurring.isActive(), recurring.getCreatedDate());
    }
}
