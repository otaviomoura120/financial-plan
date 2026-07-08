package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillInstance;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.BillInstanceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillInstanceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillInstanceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class BillInstanceRepositoryImpl implements BillInstanceRepository {

    private final JpaBillInstanceRepository jpaBillInstanceRepository;
    private final JpaBillRepository jpaBillRepository;

    public BillInstanceRepositoryImpl(JpaBillInstanceRepository jpaBillInstanceRepository, JpaBillRepository jpaBillRepository) {
        this.jpaBillInstanceRepository = jpaBillInstanceRepository;
        this.jpaBillRepository = jpaBillRepository;
    }

    @Override
    public BillInstance save(BillInstance billInstance) {
        BillInstanceEntityJpa entity = new BillInstanceEntityJpa();
        applyFields(billInstance, entity);
        entity.setCreatedAt(billInstance.getCreatedDate());
        BillInstanceEntityJpa saved = jpaBillInstanceRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public BillInstance update(BillInstance billInstance) {
        BillInstanceEntityJpa entity = jpaBillInstanceRepository.findById(billInstance.getId()).orElseThrow();
        applyFields(billInstance, entity);
        BillInstanceEntityJpa updated = jpaBillInstanceRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public BillInstance findById(Long id) {
        return jpaBillInstanceRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public BillInstance findByBillIdAndReferenceMonth(Long billId, LocalDate referenceMonth) {
        return jpaBillInstanceRepository.findByBill_IdAndReferenceMonth(billId, referenceMonth)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<BillInstance> findByBillId(Long billId) {
        return jpaBillInstanceRepository.findByBill_Id(billId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<BillInstance> findBySpaceAndPeriod(Long spaceId, LocalDate from, LocalDate to) {
        Specification<BillInstanceEntityJpa> specification = buildSpecification(spaceId, from, to);
        return jpaBillInstanceRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    private Specification<BillInstanceEntityJpa> buildSpecification(Long spaceId, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("bill").get("id").in(billIdsInSpace(spaceId, query, criteriaBuilder)));
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), to));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Subquery<Long> billIdsInSpace(Long spaceId, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<BillEntityJpa> billRoot = subquery.from(BillEntityJpa.class);
        subquery.select(billRoot.get("id"));
        subquery.where(criteriaBuilder.equal(billRoot.get("space").get("id"), spaceId));
        return subquery;
    }

    private void applyFields(BillInstance billInstance, BillInstanceEntityJpa entity) {
        entity.setBill(jpaBillRepository.getReferenceById(billInstance.getBill().getId()));
        entity.setReferenceMonth(billInstance.getReferenceMonth());
        entity.setDueDate(billInstance.getDueDate());
        entity.setAmount(billInstance.getAmount());
        entity.setStatus(billInstance.getStatus());
        entity.setPaidDate(billInstance.getPaidDate());
        entity.setPaymentTransactionId(billInstance.getPaymentTransactionId());
        entity.setBankAccountId(billInstance.getBankAccountId());
    }

    private BillInstance toDomain(BillInstanceEntityJpa entity) {
        Bill bill = entity.getBill() != null ? buildBill(entity.getBill()) : null;
        return new BillInstance(entity.getId(), entity.getVersion(), bill, entity.getReferenceMonth(), entity.getDueDate(),
                entity.getAmount(), entity.getStatus(), entity.getPaidDate(), entity.getPaymentTransactionId(),
                entity.getBankAccountId(), entity.getCreatedAt(), null);
    }

    private Bill buildBill(BillEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        return new Bill(entity.getId(), entity.getVersion(), space, entity.getName(), category, entity.getDefaultAmount(),
                entity.getStartDate(), entity.isRecurring(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Category buildCategory(CategoryEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new Category(entity.getId(), entity.getVersion(), space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
