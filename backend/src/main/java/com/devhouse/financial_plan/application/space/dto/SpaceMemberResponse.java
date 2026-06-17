package com.devhouse.financial_plan.application.space.dto;

import java.time.Instant;

public record SpaceMemberResponse(
        Long memberId,
        Integer version,
        Long userId,
        String userName,
        String userEmail,
        Long roleId,
        String roleName,
        Instant joinedAt
) {}
