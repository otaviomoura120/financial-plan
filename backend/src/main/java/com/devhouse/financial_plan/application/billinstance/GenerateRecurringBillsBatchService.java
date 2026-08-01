package com.devhouse.financial_plan.application.billinstance;

import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.repository.BillRecurringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tops up every active recurrence so the configured horizon of future bills always exists,
 * regardless of anyone opening the bills screen or a report.
 */
@Service
public class GenerateRecurringBillsBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateRecurringBillsBatchService.class);

    private final BillRecurringRepository billRecurringRepository;
    private final EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService;

    public GenerateRecurringBillsBatchService(BillRecurringRepository billRecurringRepository,
                                              EnsureRecurringBillsGeneratedService ensureRecurringBillsGeneratedService) {
        this.billRecurringRepository = billRecurringRepository;
        this.ensureRecurringBillsGeneratedService = ensureRecurringBillsGeneratedService;
    }

    public void execute() {
        List<BillRecurring> activeRecurrings = billRecurringRepository.findAllActive();
        int failures = 0;
        for (BillRecurring billRecurring : activeRecurrings) {
            if (!generateSafely(billRecurring)) {
                failures++;
            }
        }
        LOGGER.info("Recurring bills top-up finished: {} recurrences processed, {} failed", activeRecurrings.size(), failures);
    }

    private boolean generateSafely(BillRecurring billRecurring) {
        try {
            ensureRecurringBillsGeneratedService.executeForRecurring(billRecurring);
            return true;
        }
        catch (Exception exception) {
            LOGGER.error("Failed to generate bills for recurrence {}", billRecurring.getId(), exception);
            return false;
        }
    }
}
