package com.devhouse.financial_plan.application.role.dto;

public record UpdateRoleRequest(
        Integer version,
        String name,
        String description
) {}
