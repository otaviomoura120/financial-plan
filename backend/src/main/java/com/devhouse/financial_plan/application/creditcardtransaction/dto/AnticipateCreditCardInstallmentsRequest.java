package com.devhouse.financial_plan.application.creditcardtransaction.dto;

import java.time.LocalDate;

public record AnticipateCreditCardInstallmentsRequest(LocalDate targetReferenceMonth, Integer installmentsToAnticipate) {}
