package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.GroupMenu;

import java.util.List;

public interface GroupMenuRepository {

    GroupMenu save(GroupMenu groupMenu);
    GroupMenu update(GroupMenu groupMenu);
    GroupMenu findById(Long id);
    List<GroupMenu> findAllWithChildren();
    void delete(Long id);
}
