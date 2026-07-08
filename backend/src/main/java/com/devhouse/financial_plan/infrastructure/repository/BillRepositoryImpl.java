package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.BillRecurring;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillRecurringEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class BillRepositoryImpl implements BillRepository {

    private final JpaBillRepository jpaBillRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaBillRecurringRepository jpaBillRecurringRepository;
    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    public BillRepositoryImpl(JpaBillRepository jpaBillRepository, JpaSpaceRepository jpaSpaceRepository,
                               JpaBillRecurringRepository jpaBillRecurringRepository, JpaCategoryRepository jpaCategoryRepository,
                               JpaSubCategoryRepository jpaSubCategoryRepository) {
        this.jpaBillRepository = jpaBillRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaBillRecurringRepository = jpaBillRecurringRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
    }

    @Override
    public Bill save(Bill bill) {
        BillEntityJpa entity = new BillEntityJpa();
        applyFields(bill, entity);
        entity.setCreatedAt(bill.getCreatedDate());
        BillEntityJpa saved = jpaBillRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Bill update(Bill bill) {
        BillEntityJpa entity = jpaBillRepository.findById(bill.getId()).orElseThrow();
        applyFields(bill, entity);
        BillEntityJpa updated = jpaBillRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public Bill findById(Long id) {
        return jpaBillRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public Bill findByBillRecurringIdAndReferenceMonth(Long billRecurringId, LocalDate referenceMonth) {
        return jpaBillRepository.findByBillRecurring_IdAndReferenceMonth(billRecurringId, referenceMonth)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Bill> findByBillRecurringId(Long billRecurringId) {
        return jpaBillRepository.findByBillRecurring_Id(billRecurringId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Bill> findBySpaceAndPeriod(Long spaceId, LocalDate from, LocalDate to) {
        Specification<BillEntityJpa> specification = buildSpecification(spaceId, from, to);
        return jpaBillRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    private Specification<BillEntityJpa> buildSpecification(Long spaceId, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("space").get("id"), spaceId));
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), to));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyFields(Bill bill, BillEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(bill.getSpace().getId()));
        entity.setBillRecurring(bill.getBillRecurring() != null ? jpaBillRecurringRepository.getReferenceById(bill.getBillRecurring().getId()) : null);
        entity.setName(bill.getName());
        entity.setCategory(bill.getCategory() != null ? jpaCategoryRepository.getReferenceById(bill.getCategory().getId()) : null);
        entity.setSubCategory(bill.getSubCategory() != null ? jpaSubCategoryRepository.getReferenceById(bill.getSubCategory().getId()) : null);
        entity.setReferenceMonth(bill.getReferenceMonth());
        entity.setDueDate(bill.getDueDate());
        entity.setAmount(bill.getAmount());
        entity.setStatus(bill.getStatus());
        entity.setPaidDate(bill.getPaidDate());
        entity.setPaymentTransactionId(bill.getPaymentTransactionId());
        entity.setBankAccountId(bill.getBankAccountId());
        entity.setDeleted(bill.isDeleted());
    }

    private Bill toDomain(BillEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        BillRecurring billRecurring = entity.getBillRecurring() != null ? buildBillRecurring(entity.getBillRecurring()) : null;
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new Bill(entity.getId(), entity.getVersion(), space, billRecurring, entity.getName(), category, subCategory,
                entity.getReferenceMonth(), entity.getDueDate(), entity.getAmount(), entity.getStatus(), entity.getPaidDate(),
                entity.getPaymentTransactionId(), entity.getBankAccountId(), entity.isDeleted(), entity.getCreatedAt(), null);
    }

    private BillRecurring buildBillRecurring(BillRecurringEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new BillRecurring(entity.getId(), entity.getVersion(), space, entity.getName(), category, subCategory,
                entity.getDefaultAmount(), entity.getStartDate(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private SubCategory buildSubCategory(SubCategoryEntityJpa entity) {
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        return new SubCategory(entity.getId(), entity.getVersion(), category, entity.getName(), entity.isActive(), null, null);
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
