package com.devhouse.financial_plan.application.creditcardtransaction;

import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerateRecurringCreditCardTransactionsBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateRecurringCreditCardTransactionsBatchService.class);

    private final CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository;
    private final EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService;

    public GenerateRecurringCreditCardTransactionsBatchService(CreditCardTransactionRecurringRepository creditCardTransactionRecurringRepository,
                                                                EnsureRecurringCreditCardTransactionsGeneratedService ensureRecurringCreditCardTransactionsGeneratedService) {
        this.creditCardTransactionRecurringRepository = creditCardTransactionRecurringRepository;
        this.ensureRecurringCreditCardTransactionsGeneratedService = ensureRecurringCreditCardTransactionsGeneratedService;
    }

    public void execute() {
        List<CreditCardTransactionRecurring> activeRecurrings = creditCardTransactionRecurringRepository.findAllActive();
        int failures = 0;
        for (CreditCardTransactionRecurring recurring : activeRecurrings) {
            if (!generateSafely(recurring)) {
                failures++;
            }
        }
        LOGGER.info("Recurring credit card transactions top-up finished: {} recurrences processed, {} failed",
                activeRecurrings.size(), failures);
    }

    private boolean generateSafely(CreditCardTransactionRecurring recurring) {
        try {
            ensureRecurringCreditCardTransactionsGeneratedService.executeForRecurring(recurring);
            return true;
        }
        catch (Exception exception) {
            LOGGER.error("Failed to generate credit card transactions for recurrence {}", recurring.getId(), exception);
            return false;
        }
    }
}
