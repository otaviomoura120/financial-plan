package com.devhouse.financial_plan.application.groupmenu.dto;

import java.time.Instant;

public record GroupMenuChildrenResponse(
        Long id,
        Integer version,
        Long groupMenuId,
        String name,
        String endpoint,
        String icon,
        Instant createdAt,
        Instant updatedAt
) {}
