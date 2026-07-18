package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntityJpa, Long>, JpaSpecificationExecutor<TransactionEntityJpa> {

    boolean existsByBankAccountIdOrDestinationBankAccountId(Long bankAccountId, Long destinationBankAccountId);
    boolean existsByCategoryId(Long categoryId);
    boolean existsBySubCategoryId(Long subCategoryId);
}
