package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Bill;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.BillRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BillEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBillRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class BillRepositoryImpl implements BillRepository {

    private final JpaBillRepository jpaBillRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaCategoryRepository jpaCategoryRepository;

    public BillRepositoryImpl(JpaBillRepository jpaBillRepository, JpaSpaceRepository jpaSpaceRepository,
                               JpaCategoryRepository jpaCategoryRepository) {
        this.jpaBillRepository = jpaBillRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
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
    public List<Bill> findBySpaceId(Long spaceId) {
        return jpaBillRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaBillRepository.deleteById(id);
    }

    private void applyFields(Bill bill, BillEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(bill.getSpace().getId()));
        entity.setName(bill.getName());
        entity.setCategory(bill.getCategory() != null ? jpaCategoryRepository.getReferenceById(bill.getCategory().getId()) : null);
        entity.setDefaultAmount(bill.getDefaultAmount());
        entity.setStartDate(bill.getStartDate());
        entity.setRecurring(bill.isRecurring());
        entity.setActive(bill.isActive());
    }

    private Bill toDomain(BillEntityJpa entity) {
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
