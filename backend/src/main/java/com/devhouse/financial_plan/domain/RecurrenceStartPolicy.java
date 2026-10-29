package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;

import java.time.LocalDate;

/**
 * Recurring bills and card subscriptions materialize one row per month from their start date, so a start date
 * far in the past would create thousands of rows in a single request.
 *
 * The limit applies when a start date is chosen (creation, or an edit that changes it). A recurrence that ages
 * past the limit keeps its start date and stays editable.
 */
public final class RecurrenceStartPolicy {

    public static final int MAX_YEARS_IN_PAST = 5;

    private RecurrenceStartPolicy() {
    }

    public static void requireRecentStart(LocalDate startDate) {
        if (startDate == null) {
            return;
        }
        if (startDate.isBefore(LocalDate.now().minusYears(MAX_YEARS_IN_PAST))) {
            throw new DomainException("Start date cannot be more than " + MAX_YEARS_IN_PAST + " years in the past");
        }
    }

    public static void requireRecentStartIfChanged(LocalDate currentStartDate, LocalDate newStartDate) {
        if (newStartDate != null && newStartDate.equals(currentStartDate)) {
            return;
        }
        requireRecentStart(newStartDate);
    }
}
