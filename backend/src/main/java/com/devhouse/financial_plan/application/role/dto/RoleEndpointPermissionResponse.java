package com.devhouse.financial_plan.application.role.dto;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

public record RoleEndpointPermissionResponse(
        Long id,
        Integer version,
        Long endpointPermissionId,
        String name,
        String endpoint,
        EndpointPermissionType type,
        String group,
        EndpointPermissionAccess permission
) {}
