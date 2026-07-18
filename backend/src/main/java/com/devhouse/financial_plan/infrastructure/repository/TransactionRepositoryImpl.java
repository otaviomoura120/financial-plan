package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.Transaction;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.domain.repository.TransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBankAccountRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaTransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.TransactionEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import jakarta.persistence.criteria.Subquery;
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
    private final JpaUserRepository jpaUserRepository;
    private final JpaBankAccountRepository jpaBankAccountRepository;
    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    public TransactionRepositoryImpl(JpaTransactionRepository jpaTransactionRepository, JpaUserRepository jpaUserRepository,
                                      JpaBankAccountRepository jpaBankAccountRepository, JpaCategoryRepository jpaCategoryRepository,
                                      JpaSubCategoryRepository jpaSubCategoryRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaBankAccountRepository = jpaBankAccountRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntityJpa entity = new TransactionEntityJpa();
        applyFields(transaction, entity);
        entity.setCreatedAt(transaction.getCreatedDate());
        TransactionEntityJpa saved = jpaTransactionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Transaction update(Transaction transaction) {
        TransactionEntityJpa entity = jpaTransactionRepository.findById(transaction.getId()).orElseThrow();
        applyFields(transaction, entity);
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
    public List<Transaction> findByFilter(Long spaceId, Long userId, Long bankAccountId, Long categoryId, Long subCategoryId,
                                          TransactionType type, LocalDate from, LocalDate to) {
        Specification<TransactionEntityJpa> specification = buildSpecification(spaceId, userId, bankAccountId, categoryId,
                subCategoryId, type, from, to);
        return jpaTransactionRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaTransactionRepository.deleteById(id);
    }

    @Override
    public boolean existsByBankAccountId(Long bankAccountId) {
        return jpaTransactionRepository.existsByBankAccountIdOrDestinationBankAccountId(bankAccountId, bankAccountId);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return jpaTransactionRepository.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsBySubCategoryId(Long subCategoryId) {
        return jpaTransactionRepository.existsBySubCategoryId(subCategoryId);
    }

    private Specification<TransactionEntityJpa> buildSpecification(Long spaceId, Long userId, Long bankAccountId, Long categoryId,
                                                                     Long subCategoryId,
                                                                     TransactionType type, LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("bankAccount").get("id").in(bankAccountIdsInSpace(spaceId, query, criteriaBuilder)));
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }
            if (bankAccountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("bankAccount").get("id"), bankAccountId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subCategory").get("id"), subCategoryId));
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

    private Subquery<Long> bankAccountIdsInSpace(Long spaceId, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                  jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
        Subquery<Long> subquery = query.subquery(Long.class);
        jakarta.persistence.criteria.Root<BankAccountEntityJpa> bankAccountRoot = subquery.from(BankAccountEntityJpa.class);
        subquery.select(bankAccountRoot.get("id"));
        subquery.where(criteriaBuilder.equal(bankAccountRoot.get("space").get("id"), spaceId));
        return subquery;
    }

    private void applyFields(Transaction transaction, TransactionEntityJpa entity) {
        entity.setType(transaction.getType());
        entity.setUser(jpaUserRepository.getReferenceById(transaction.getUser().getId()));
        entity.setBankAccount(jpaBankAccountRepository.getReferenceById(transaction.getBankAccount().getId()));
        entity.setDestinationBankAccount(transaction.getDestinationBankAccount() != null
                ? jpaBankAccountRepository.getReferenceById(transaction.getDestinationBankAccount().getId()) : null);
        entity.setCategory(transaction.getCategory() != null
                ? jpaCategoryRepository.getReferenceById(transaction.getCategory().getId()) : null);
        entity.setSubCategory(transaction.getSubCategory() != null
                ? jpaSubCategoryRepository.getReferenceById(transaction.getSubCategory().getId()) : null);
        entity.setAmount(transaction.getAmount());
        entity.setTransactionDate(transaction.getTransactionDate());
        entity.setDescription(transaction.getDescription());
        entity.setSourceType(transaction.getSourceType());
        entity.setSourceId(transaction.getSourceId());
    }

    private Transaction toDomain(TransactionEntityJpa entity) {
        User user = buildUser(entity.getUser());
        BankAccount bankAccount = buildBankAccount(entity.getBankAccount());
        BankAccount destinationBankAccount = entity.getDestinationBankAccount() != null ? buildBankAccount(entity.getDestinationBankAccount()) : null;
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new Transaction(entity.getId(), entity.getVersion(), entity.getType(), user, bankAccount,
                destinationBankAccount, category, subCategory, entity.getAmount(),
                entity.getTransactionDate(), entity.getDescription(), entity.getCreatedAt(), null,
                entity.getSourceType(), entity.getSourceId());
    }

    private User buildUser(UserEntityJpa entity) {
        if (entity == null) {
            return null;
        }
        return new User(entity.getId(), entity.getVersion(), entity.getAuth0Sub(), entity.getName(), entity.getNickname(),
                entity.getProfilePhoto(), entity.getObservation(), entity.getBirthdate(), entity.getEmail(),
                entity.getPhoneNumber(), entity.isActive(), entity.getGenre(), entity.getMaritalStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.isMasterAdmin());
    }

    private BankAccount buildBankAccount(BankAccountEntityJpa entity) {
        if (entity == null) {
            return null;
        }
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new BankAccount(entity.getId(), entity.getVersion(), space, entity.getName(), entity.getBankName(),
                entity.getBalance(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Category buildCategory(CategoryEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new Category(entity.getId(), entity.getVersion(), space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private SubCategory buildSubCategory(SubCategoryEntityJpa entity) {
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        return new SubCategory(entity.getId(), entity.getVersion(), category, entity.getName(), entity.isActive(), null, null);
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
