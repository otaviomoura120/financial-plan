package com.devhouse.financial_plan.application.space.dto;

import com.devhouse.financial_plan.domain.enums.InviteStatus;

import java.time.Instant;

public record SpaceInviteResponse(
        Long inviteId,
        String email,
        Long roleId,
        String roleName,
        InviteStatus status,
        Instant createdAt,
        Instant expiresAt
) {}
