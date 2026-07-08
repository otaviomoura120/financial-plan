package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaBillRepository extends JpaRepository<BillEntityJpa, Long>,
        JpaSpecificationExecutor<BillEntityJpa> {

    Optional<BillEntityJpa> findByBillRecurring_IdAndReferenceMonth(Long billRecurringId, LocalDate referenceMonth);
    List<BillEntityJpa> findByBillRecurring_Id(Long billRecurringId);
}
