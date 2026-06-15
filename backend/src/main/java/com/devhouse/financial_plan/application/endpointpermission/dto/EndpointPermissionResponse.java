package com.devhouse.financial_plan.application.endpointpermission.dto;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

import java.time.Instant;

public record EndpointPermissionResponse(
        Long id,
        Integer version,
        String endpoint,
        String name,
        String icon,
        Integer sequence,
        EndpointPermissionType type,
        String permittedMethods,
        Instant createdAt,
        Instant updatedAt
) {}
