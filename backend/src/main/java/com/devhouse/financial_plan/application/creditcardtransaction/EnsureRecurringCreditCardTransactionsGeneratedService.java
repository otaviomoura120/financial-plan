package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardInvoiceCycle;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.repository.CreditCardInvoicePaymentRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.UUID;

@Service
public class EnsureRecurringCreditCardTransactionsGeneratedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnsureRecurringCreditCardTransactionsGeneratedService.class);

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository;
    private final int horizonMonths;
    private final int maxHorizonMonths;

    public EnsureRecurringCreditCardTransactionsGeneratedService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                                  CreditCardTransactionRepository creditCardTransactionRepository,
                                                                  CreditCardInvoicePaymentRepository creditCardInvoicePaymentRepository,
                                                                  @Value("${credit-cards.recurring.horizon-months:12}") int horizonMonths,
                                                                  @Value("${credit-cards.recurring.max-horizon-months:60}") int maxHorizonMonths) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.creditCardTransactionRepository = creditCardTransactionRepository;
        this.creditCardInvoicePaymentRepository = creditCardInvoicePaymentRepository;
        this.horizonMonths = horizonMonths;
        this.maxHorizonMonths = maxHorizonMonths;
    }

    @Transactional
    public void execute(Long spaceId, LocalDate upToDate) {
        YearMonth capMonth = resolveCapMonth(upToDate);
        creditCardTransactionRecurringRepository.findBySpaceId(spaceId).stream()
                .filter(CreditCardTransactionRecurring::isActive)
                .forEach(recurring -> generateMissingTransactions(recurring, capMonth));
    }

    @Transactional
    public void executeForRecurring(CreditCardTransactionRecurring recurring) {
        if (!recurring.isActive()) {
            return;
        }
        generateMissingTransactions(recurring, defaultHorizonMonth());
    }

    private YearMonth defaultHorizonMonth() {
        return YearMonth.now().plusMonths(horizonMonths);
    }

    /**
     * Always keeps at least {@code horizonMonths} of future subscription charges materialized. A period
     * filter reaching further ahead is honoured, up to a safety ceiling so an extreme filter cannot
     * create thousands of rows inside a single request.
     */
    private YearMonth resolveCapMonth(LocalDate upToDate) {
        YearMonth requestedMonth = YearMonth.from(upToDate);
        YearMonth horizonMonth = defaultHorizonMonth();
        YearMonth ceilingMonth = YearMonth.now().plusMonths(maxHorizonMonths);
        YearMonth capMonth = requestedMonth.isAfter(horizonMonth) ? requestedMonth : horizonMonth;
        return capMonth.isAfter(ceilingMonth) ? ceilingMonth : capMonth;
    }

    private void generateMissingTransactions(CreditCardTransactionRecurring recurring, YearMonth capMonth) {
        YearMonth cursor = resolveFirstMonthToGenerate(recurring);
        while (!cursor.isAfter(capMonth)) {
            createTransactionIfMissing(recurring, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    private YearMonth resolveFirstMonthToGenerate(CreditCardTransactionRecurring recurring) {
        YearMonth startMonth = YearMonth.from(recurring.getStartDate());
        YearMonth lastGeneratedMonth = creditCardTransactionRepository.findByCreditCardTransactionRecurringId(recurring.getId()).stream()
                .map(CreditCardTransaction::getPurchaseDate)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(startMonth.minusMonths(1));

        YearMonth cursor = lastGeneratedMonth.plusMonths(1);
        return cursor.isBefore(startMonth) ? startMonth : cursor;
    }

    private void createTransactionIfMissing(CreditCardTransactionRecurring recurring, YearMonth month) {
        if (!creditCardTransactionRepository.findByCreditCardTransactionRecurringIdAndPurchaseMonth(recurring.getId(), month).isEmpty()) {
            return;
        }
        LocalDate purchaseDate = resolvePurchaseDate(recurring, month);
        LocalDate referenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, recurring.getCreditCard().getClosingDay());
        if (isInvoiceAlreadyPaid(recurring, referenceMonth)) {
            LOGGER.debug("Skipping month {} of recurrence {}: invoice {} is already paid", month, recurring.getId(), referenceMonth);
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
