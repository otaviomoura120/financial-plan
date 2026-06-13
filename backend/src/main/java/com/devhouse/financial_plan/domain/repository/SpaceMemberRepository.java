package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.SpaceMember;

import java.util.List;

public interface SpaceMemberRepository {
    SpaceMember save(SpaceMember member);
    SpaceMember update(SpaceMember member);
    SpaceMember findBySpaceIdAndUserId(Long spaceId, Long userId);
    List<SpaceMember> findBySpaceId(Long spaceId);
    List<SpaceMember> findByUserId(Long userId);
    void delete(Long id);
}
