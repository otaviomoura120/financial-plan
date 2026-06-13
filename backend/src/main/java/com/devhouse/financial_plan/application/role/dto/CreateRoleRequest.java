package com.devhouse.financial_plan.application.role.dto;

public record CreateRoleRequest(
        Long spaceId,
        String name,
        String description
) {}
