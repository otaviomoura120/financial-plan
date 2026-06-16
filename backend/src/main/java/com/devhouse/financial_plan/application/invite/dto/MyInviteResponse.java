package com.devhouse.financial_plan.application.invite.dto;

import java.time.Instant;

public record MyInviteResponse(
        String token,
        Long spaceId,
        String spaceName,
        Long roleId,
        String roleName,
        Instant expiresAt
) {}
