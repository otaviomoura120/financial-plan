package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaBankAccountRepository extends JpaRepository<BankAccountEntityJpa, Long> {

    List<BankAccountEntityJpa> findBySpaceId(Long spaceId);
}
