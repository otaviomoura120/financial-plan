package com.devhouse.financial_plan.domain.repository;

import com.devhouse.financial_plan.domain.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);
    User update(User user);
    User findById(Long id);
    User findByAuth0Sub(String auth0Sub);
    Optional<User> findByEmail(String email);
    void delete(Long id);
}
