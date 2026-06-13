package com.devhouse.financial_plan.application.family.dto;

import java.time.Instant;

public record FamilyResponse(Long id, String name, Instant createdDate) {}
