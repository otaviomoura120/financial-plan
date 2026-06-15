package com.devhouse.financial_plan.application.endpointpermission.dto;

import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

public record CreateEndpointPermissionRequest(
        String endpoint,
        String name,
        String icon,
        Integer sequence,
        EndpointPermissionType type,
        String permittedMethods,
        String group
) {}
