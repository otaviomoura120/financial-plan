package com.devhouse.financial_plan.application.groupmenu.dto;

import java.time.Instant;
import java.util.List;

public record GroupMenuWithChildrenResponse(
        Long id,
        Integer version,
        String name,
        String icon,
        List<GroupMenuChildrenResponse> children,
        Instant createdAt,
        Instant updatedAt
) {}
