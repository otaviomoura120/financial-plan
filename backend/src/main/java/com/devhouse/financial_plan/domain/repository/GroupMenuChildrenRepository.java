package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.GroupMenuChildren;

import java.util.List;

public interface GroupMenuChildrenRepository {

    GroupMenuChildren save(GroupMenuChildren child);
    GroupMenuChildren update(GroupMenuChildren child);
    GroupMenuChildren findById(Long id);
    List<GroupMenuChildren> findByGroupMenuId(Long groupMenuId);
    void delete(Long id);
}
