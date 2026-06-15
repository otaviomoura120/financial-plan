package com.devhouse.financial_plan.application.role.dto;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;

public record UpdateRolePermissionAccessRequest(
        Integer version,
        EndpointPermissionAccess access
) {}
