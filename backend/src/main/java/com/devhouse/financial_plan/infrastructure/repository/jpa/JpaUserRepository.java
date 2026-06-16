package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntityJpa, Long> {

    Optional<UserEntityJpa> findByAuth0Sub(String auth0Sub);
    Optional<UserEntityJpa> findByEmail(String email);
}
