package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaTransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.TransactionEntityJpa;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class TransactionRepositoryImpl implements TransactionRepository {

    private final JpaTransactionRepository jpaTransactionRepository;

    public TransactionRepositoryImpl(JpaTransactionRepository jpaTransactionRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntityJpa entity = new TransactionEntityJpa();
        applyFields(transaction, entity);
        TransactionEntityJpa saved = jpaTransactionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Transaction update(Transaction transaction) {
        TransactionEntityJpa entity = jpaTransactionRepository.findById(transaction.getId()).orElseThrow();
        entity.setType(transaction.getType());
        entity.setBankAccountId(transaction.getBankAccountId());
        entity.setDestinationBankAccountId(transaction.getDestinationBankAccountId());
        entity.setCategoryId(transaction.getCategoryId());
        entity.setSubCategoryId(transaction.getSubCategoryId());
        entity.setPaymentMethodId(transaction.getPaymentMethodId());
        entity.setAmount(transaction.getAmount());
        entity.setTransactionDate(transaction.getTransactionDate());
        entity.setDescription(transaction.getDescription());
        TransactionEntityJpa updated = jpaTransactionRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public Transaction findById(Long id) {
        return jpaTransactionRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<Transaction> findByFilter(Long userId, Long bankAccountId, Long categoryId, Long subCategoryId,
                                          Long paymentMethodId, TransactionType type, LocalDate from, LocalDate to) {
        Specification<TransactionEntityJpa> specification = buildSpecification(userId, bankAccountId, categoryId,
                subCategoryId, paymentMethodId, type, from, to);
        return jpaTransactionRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaTransactionRepository.deleteById(id);
    }

    private Specification<TransactionEntityJpa> buildSpecification(Long userId, Long bankAccountId, Long categoryId,
                                                                     Long subCategoryId, Long paymentMethodId,
                                                                     TransactionType type, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (bankAccountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("bankAccountId"), bankAccountId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subCategoryId"), subCategoryId));
            }
            if (paymentMethodId != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethodId"), paymentMethodId));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void applyFields(Transaction transaction, TransactionEntityJpa entity) {
        entity.setType(transaction.getType());
        entity.setUserId(transaction.getUserId());
        entity.setBankAccountId(transaction.getBankAccountId());
        entity.setDestinationBankAccountId(transaction.getDestinationBankAccountId());
        entity.setCategoryId(transaction.getCategoryId());
        entity.setSubCategoryId(transaction.getSubCategoryId());
        entity.setPaymentMethodId(transaction.getPaymentMethodId());
        entity.setAmount(transaction.getAmount());
        entity.setTransactionDate(transaction.getTransactionDate());
        entity.setDescription(transaction.getDescription());
        entity.setCreatedAt(transaction.getCreatedDate());
    }

    private Transaction toDomain(TransactionEntityJpa entity) {
        return new Transaction(entity.getId(), entity.getVersion(), entity.getType(), entity.getUserId(),
                entity.getBankAccountId(), entity.getDestinationBankAccountId(), entity.getCategoryId(),
                entity.getSubCategoryId(), entity.getPaymentMethodId(), entity.getAmount(),
                entity.getTransactionDate(), entity.getDescription(), entity.getCreatedAt(), null);
    }
}
