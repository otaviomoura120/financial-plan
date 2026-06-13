package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType;

import java.util.List;

public interface EndpointPermissionRepository {

    EndpointPermission save(EndpointPermission permission);
    EndpointPermission update(EndpointPermission permission);
    EndpointPermission findById(Long id);
    List<EndpointPermission> findByType(EndpointPermissionType type);
    List<EndpointPermission> findAllOrderedBySequence();
    void delete(Long id);
}
