package com.devhouse.financial_plan.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaBillInstanceRepository extends JpaRepository<BillInstanceEntityJpa, Long>,
        JpaSpecificationExecutor<BillInstanceEntityJpa> {

    Optional<BillInstanceEntityJpa> findByBill_IdAndReferenceMonth(Long billId, LocalDate referenceMonth);
    List<BillInstanceEntityJpa> findByBill_Id(Long billId);
}
