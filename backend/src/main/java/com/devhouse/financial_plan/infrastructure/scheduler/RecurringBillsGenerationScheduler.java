package com.devhouse.financial_plan.infrastructure.scheduler;

import com.devhouse.financial_plan.application.billinstance.GenerateRecurringBillsBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringBillsGenerationScheduler {

    private final GenerateRecurringBillsBatchService generateRecurringBillsBatchService;

    public RecurringBillsGenerationScheduler(GenerateRecurringBillsBatchService generateRecurringBillsBatchService) {
        this.generateRecurringBillsBatchService = generateRecurringBillsBatchService;
    }

    @Scheduled(cron = "${bills.recurring.generation-cron:0 0 3 * * *}")
    public void topUpRecurringBills() {
        generateRecurringBillsBatchService.execute();
    }
}
