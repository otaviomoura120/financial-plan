package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Role;

import java.util.List;

public interface RoleRepository {

    Role save(Role role);
    Role update(Role role);
    Role findById(Long id);
    List<Role> findBySpaceId(Long spaceId);
    void delete(Long id);
}
