package com.devhouse.financial_plan.application.role.dto;

import java.time.Instant;

public record RoleResponse(
        Long id,
        Integer version,
        Long spaceId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
