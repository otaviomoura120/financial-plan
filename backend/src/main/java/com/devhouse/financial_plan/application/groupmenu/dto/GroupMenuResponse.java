package com.devhouse.financial_plan.application.groupmenu.dto;

import java.time.Instant;

public record GroupMenuResponse(
        Long id,
        String name,
        String icon,
        Instant createdAt,
        Instant updatedAt
) {}
