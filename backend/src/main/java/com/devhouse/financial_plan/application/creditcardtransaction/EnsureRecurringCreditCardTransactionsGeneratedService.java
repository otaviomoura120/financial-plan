package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.UUID;

@Service
public class EnsureRecurringCreditCardTransactionsGeneratedService {

    private static final int MINIMUM_MONTHS_AHEAD = 6;

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;

    public EnsureRecurringCreditCardTransactionsGeneratedService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                                  CreditCardTransactionRepository creditCardTransactionRepository,
                                                                  CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
    }

    @Transactional
    public void execute(Long spaceId, LocalDate upToDate) {
        YearMonth capMonth = resolveCapMonth(upToDate);
        creditCardTransactionRecurringRepository.findBySpaceId(spaceId).stream()
                .filter(CreditCardTransactionRecurring::isActive)
                .forEach(recurring -> generateMissingTransactions(recurring, capMonth));
    }

    private YearMonth resolveCapMonth(LocalDate upToDate) {
        YearMonth requestedMonth = YearMonth.from(upToDate);
        YearMonth minimumMonth = YearMonth.now().plusMonths(MINIMUM_MONTHS_AHEAD);
        return requestedMonth.isAfter(minimumMonth) ? requestedMonth : minimumMonth;
    }

    private void generateMissingTransactions(CreditCardTransactionRecurring recurring, YearMonth capMonth) {
        YearMonth startMonth = YearMonth.from(recurring.getStartDate());
        YearMonth lastGeneratedMonth = creditCardTransactionRepository.findByCreditCardTransactionRecurringId(recurring.getId()).stream()
                .map(CreditCardTransaction::getPurchaseDate)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(startMonth.minusMonths(1));

        YearMonth cursor = lastGeneratedMonth.plusMonths(1);
        if (cursor.isBefore(startMonth)) {
            cursor = startMonth;
        }
        while (!cursor.isAfter(capMonth)) {
            createTransactionIfMissing(recurring, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    private void createTransactionIfMissing(CreditCardTransactionRecurring recurring, YearMonth month) {
        if (!creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(recurring.getId(), month).isEmpty()) {
            return;
        }
        LocalDate purchaseDate = resolvePurchaseDate(recurring, month);
        LocalDate referenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, recurring.getCreditCard().getClosingDay());
        if (isInvoiceAlreadyPaid(recurring, referenceMonth)) {
            return;
        }
        CreditCardTransaction transaction = new CreditCardTransaction(null, 0, recurring.getCreditCard(), recurring, recurring.getUser(),
                recurring.getCategory(), recurring.getSubCategory(), recurring.getDefaultAmount(), false, purchaseDate,
                recurring.getDescription(), referenceMonth, UUID.randomUUID().toString(), 1, 1, false, null, Instant.now(), null);
        transaction.validate();
        creditCardTransactionRepository.save(transaction);
    }

    private boolean isInvoiceAlreadyPaid(CreditCardTransactionRecurring recurring, LocalDate referenceMonth) {
        return creditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth(recurring.getCreditCard().getId(), referenceMonth) != null;
    }

    private LocalDate resolvePurchaseDate(CreditCardTransactionRecurring recurring, YearMonth month) {
        int dayOfMonth = Math.min(recurring.getStartDate().getDayOfMonth(), month.lengthOfMonth());
        return month.atDay(dayOfMonth);
    }
}
