package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardTransaction;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardTransactionEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardTransactionRecurringEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardTransactionRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class CreditCardTransactionRepositoryImpl implements CreditCardTransactionRepository {

    private final JpaCreditCardTransactionRepository jpaCreditCardTransactionRepository;
    private final JpaCreditCardRepository jpaCreditCardRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;
    private final JpaCreditCardTransactionRecurringRepository jpaCreditCardTransactionRecurringRepository;

    public CreditCardTransactionRepositoryImpl(JpaCreditCardTransactionRepository jpaCreditCardTransactionRepository,
                                                JpaCreditCardRepository jpaCreditCardRepository, JpaUserRepository jpaUserRepository,
                                                JpaCategoryRepository jpaCategoryRepository, JpaSubCategoryRepository jpaSubCategoryRepository,
                                                JpaCreditCardTransactionRecurringRepository jpaCreditCardTransactionRecurringRepository) {
        this.jpaCreditCardTransactionRepository = jpaCreditCardTransactionRepository;
        this.jpaCreditCardRepository = jpaCreditCardRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
        this.jpaCreditCardTransactionRecurringRepository = jpaCreditCardTransactionRecurringRepository;
    }

    @Override
    public CreditCardTransaction save(CreditCardTransaction creditCardTransaction) {
        CreditCardTransactionEntityJpa entity = new CreditCardTransactionEntityJpa();
        applyFields(creditCardTransaction, entity);
        entity.setCreatedAt(creditCardTransaction.getCreatedDate());
        CreditCardTransactionEntityJpa saved = jpaCreditCardTransactionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public CreditCardTransaction update(CreditCardTransaction creditCardTransaction) {
        CreditCardTransactionEntityJpa entity = jpaCreditCardTransactionRepository.findById(creditCardTransaction.getId()).orElseThrow();
        applyFields(creditCardTransaction, entity);
        CreditCardTransactionEntityJpa updated = jpaCreditCardTransactionRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public CreditCardTransaction findById(Long id) {
        return jpaCreditCardTransactionRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<CreditCardTransaction> findByFilter(Long spaceId, Long creditCardId, Long categoryId, Long subCategoryId, Long userId, LocalDate referenceMonth) {
        Specification<CreditCardTransactionEntityJpa> specification = buildSpecification(spaceId, creditCardId, categoryId, subCategoryId, userId, referenceMonth);
        return jpaCreditCardTransactionRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CreditCardTransaction> findByInstallmentGroupId(String installmentGroupId) {
        return jpaCreditCardTransactionRepository.findByInstallmentGroupId(installmentGroupId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CreditCardTransaction> findByCreditCardId(Long creditCardId) {
        return jpaCreditCardTransactionRepository.findByCreditCard_Id(creditCardId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CreditCardTransaction> findByCreditCardIdAndReferenceMonth(Long creditCardId, LocalDate referenceMonth) {
        return jpaCreditCardTransactionRepository.findByCreditCard_IdAndReferenceMonth(creditCardId, referenceMonth).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCreditCardId(Long creditCardId) {
        return jpaCreditCardTransactionRepository.existsByCreditCard_Id(creditCardId);
    }

    @Override
    public List<CreditCardTransaction> findByCreditCardTransactionRecurringId(Long creditCardTransactionRecurringId) {
        return jpaCreditCardTransactionRepository.findByCreditCardTransactionRecurring_Id(creditCardTransactionRecurringId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CreditCardTransaction> findByCreditCardTransactionRecurringIdAndPurchaseMonth(Long creditCardTransactionRecurringId, YearMonth month) {
        return jpaCreditCardTransactionRepository.findByCreditCardTransactionRecurring_IdAndPurchaseDateBetween(
                        creditCardTransactionRecurringId, month.atDay(1), month.atEndOfMonth()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaCreditCardTransactionRepository.deleteById(id);
    }

    private Specification<CreditCardTransactionEntityJpa> buildSpecification(Long spaceId, Long creditCardId, Long categoryId,
                                                                              Long subCategoryId, Long userId, LocalDate referenceMonth) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("creditCard").get("id").in(creditCardIdsInSpace(spaceId, query, criteriaBuilder)));
            if (creditCardId != null) {
                predicates.add(criteriaBuilder.equal(root.get("creditCard").get("id"), creditCardId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subCategory").get("id"), subCategoryId));
            }
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }
            if (referenceMonth != null) {
                predicates.add(criteriaBuilder.equal(root.get("referenceMonth"), referenceMonth));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Subquery<Long> creditCardIdsInSpace(Long spaceId, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<CreditCardEntityJpa> creditCardRoot = subquery.from(CreditCardEntityJpa.class);
        subquery.select(creditCardRoot.get("id"));
        subquery.where(criteriaBuilder.equal(creditCardRoot.get("space").get("id"), spaceId));
        return subquery;
    }

    private void applyFields(CreditCardTransaction creditCardTransaction, CreditCardTransactionEntityJpa entity) {
        entity.setCreditCard(jpaCreditCardRepository.getReferenceById(creditCardTransaction.getCreditCard().getId()));
        entity.setCreditCardTransactionRecurring(creditCardTransaction.getCreditCardTransactionRecurring() != null
                ? jpaCreditCardTransactionRecurringRepository.getReferenceById(creditCardTransaction.getCreditCardTransactionRecurring().getId()) : null);
        entity.setUser(jpaUserRepository.getReferenceById(creditCardTransaction.getUser().getId()));
        entity.setCategory(jpaCategoryRepository.getReferenceById(creditCardTransaction.getCategory().getId()));
        entity.setSubCategory(creditCardTransaction.getSubCategory() != null
                ? jpaSubCategoryRepository.getReferenceById(creditCardTransaction.getSubCategory().getId()) : null);
        entity.setAmount(creditCardTransaction.getAmount());
        entity.setPurchaseDate(creditCardTransaction.getPurchaseDate());
        entity.setDescription(creditCardTransaction.getDescription());
        entity.setReferenceMonth(creditCardTransaction.getReferenceMonth());
        entity.setInstallmentGroupId(creditCardTransaction.getInstallmentGroupId());
        entity.setInstallmentNumber(creditCardTransaction.getInstallmentNumber());
        entity.setTotalInstallments(creditCardTransaction.getTotalInstallments());
        entity.setAnticipated(creditCardTransaction.isAnticipated());
        entity.setOriginalReferenceMonth(creditCardTransaction.getOriginalReferenceMonth());
    }

    private CreditCardTransaction toDomain(CreditCardTransactionEntityJpa entity) {
        CreditCard creditCard = buildCreditCard(entity.getCreditCard());
        CreditCardTransactionRecurring recurring = entity.getCreditCardTransactionRecurring() != null
                ? buildCreditCardTransactionRecurring(entity.getCreditCardTransactionRecurring()) : null;
        User user = buildUser(entity.getUser());
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new CreditCardTransaction(entity.getId(), entity.getVersion(), creditCard, recurring, user, category, subCategory,
                entity.getAmount(), entity.getPurchaseDate(), entity.getDescription(), entity.getReferenceMonth(),
                entity.getInstallmentGroupId(), entity.getInstallmentNumber(), entity.getTotalInstallments(),
                entity.isAnticipated(), entity.getOriginalReferenceMonth(), entity.getCreatedAt(), null);
    }

    private CreditCardTransactionRecurring buildCreditCardTransactionRecurring(CreditCardTransactionRecurringEntityJpa entity) {
        CreditCard creditCard = buildCreditCard(entity.getCreditCard());
        User user = buildUser(entity.getUser());
        Category category = entity.getCategory() != null ? buildCategory(entity.getCategory()) : null;
        SubCategory subCategory = entity.getSubCategory() != null ? buildSubCategory(entity.getSubCategory()) : null;
        return new CreditCardTransactionRecurring(entity.getId(), entity.getVersion(), creditCard, user, category, subCategory,
                entity.getDescription(), entity.getDefaultAmount(), entity.getStartDate(), entity.isActive(),
                entity.getCreatedAt(), null);
    }

    private CreditCard buildCreditCard(CreditCardEntityJpa entity) {
        if (entity == null) {
            return null;
        }
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        BankAccount bankAccount = entity.getBankAccount() != null ? buildBankAccount(entity.getBankAccount()) : null;
        return new CreditCard(entity.getId(), entity.getVersion(), space, bankAccount, entity.getName(), entity.getLimit(),
                entity.getClosingDay(), entity.getDueDay(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private BankAccount buildBankAccount(BankAccountEntityJpa entity) {
        return new BankAccount(entity.getId(), entity.getVersion(), null, entity.getName(),
                entity.getBankName(), entity.getBalance(), entity.isActive(), entity.getCreatedAt(), null);
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
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
