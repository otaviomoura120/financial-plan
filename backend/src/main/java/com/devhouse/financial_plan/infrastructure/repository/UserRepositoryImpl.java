package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryImpl implements UserRepository {

    @Override
    public User save(User user) {
        return null;
    }

    @Override
    public User update(User user) {
        return null;
    }

    @Override
    public User findById(Long id) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
