package com.devhouse.financial_plan.application.invite.dto;

public record AcceptInviteResponse(
        Long spaceId,
        String spaceName,
        Long roleId,
        String roleName,
        Long userId
) {}
