package com.devhouse.financial_plan.application.endpointpermission.dto;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

public record UpdateEndpointPermissionRequest(
        Integer version,
        String endpoint,
        String name,
        String icon,
        Integer sequence,
        EndpointPermissionType type,
        String permittedMethods
) {}
