package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.User;

public interface UserRepository {

    User save(User user);
    User update(User user);
    User findById(Long id);
    void delete(Long id);
}
