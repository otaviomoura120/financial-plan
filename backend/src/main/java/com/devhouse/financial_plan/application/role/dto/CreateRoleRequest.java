package com.devhouse.financial_plan.application.role.dto;

public record CreateRoleRequest(
        Long familyId,
        String name,
        String description
) {}
