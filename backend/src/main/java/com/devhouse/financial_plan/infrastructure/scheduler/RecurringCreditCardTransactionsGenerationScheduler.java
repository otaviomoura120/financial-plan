package com.devhouse.financial_plan.infrastructure.scheduler;

import com.devhouse.financial_plan.application.creditcardtransaction.GenerateRecurringCreditCardTransactionsBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringCreditCardTransactionsGenerationScheduler {

    private final GenerateRecurringCreditCardTransactionsBatchService generateRecurringCreditCardTransactionsBatchService;

    public RecurringCreditCardTransactionsGenerationScheduler(
            GenerateRecurringCreditCardTransactionsBatchService generateRecurringCreditCardTransactionsBatchService) {
        this.generateRecurringCreditCardTransactionsBatchService = generateRecurringCreditCardTransactionsBatchService;
    }

    @Scheduled(cron = "${credit-cards.recurring.generation-cron:0 30 3 * * *}")
    public void topUpRecurringCreditCardTransactions() {
        generateRecurringCreditCardTransactionsBatchService.execute();
    }
}
