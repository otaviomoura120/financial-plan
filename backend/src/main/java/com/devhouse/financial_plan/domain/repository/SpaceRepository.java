package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.Space;

import java.util.List;

public interface SpaceRepository {
    Space save(Space space);
    Space update(Space space);
    Space findById(Long id);
    List<Space> findByUserId(Long userId);
    void delete(Long id);
}
