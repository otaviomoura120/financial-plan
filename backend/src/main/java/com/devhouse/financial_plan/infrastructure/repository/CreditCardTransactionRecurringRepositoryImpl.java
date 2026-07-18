package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Category;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.CreditCardTransactionRecurring;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SubCategory;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.CreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardTransactionRecurringEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardTransactionRecurringRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSubCategoryRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SubCategoryEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class CreditCardTransactionRecurringRepositoryImpl implements CreditCardTransactionRecurringRepository {

    private final JpaCreditCardTransactionRecurringRepository jpaCreditCardTransactionRecurringRepository;
    private final JpaCreditCardRepository jpaCreditCardRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    public CreditCardTransactionRecurringRepositoryImpl(JpaCreditCardTransactionRecurringRepository jpaCreditCardTransactionRecurringRepository,
                                                          JpaCreditCardRepository jpaCreditCardRepository, JpaUserRepository jpaUserRepository,
                                                          JpaCategoryRepository jpaCategoryRepository, JpaSubCategoryRepository jpaSubCategoryRepository) {
        this.jpaCreditCardTransactionRecurringRepository = jpaCreditCardTransactionRecurringRepository;
        this.jpaCreditCardRepository = jpaCreditCardRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaCategoryRepository = jpaCategoryRepository;
        this.jpaSubCategoryRepository = jpaSubCategoryRepository;
    }

    @Override
    public CreditCardTransactionRecurring save(CreditCardTransactionRecurring creditCardTransactionRecurring) {
        CreditCardTransactionRecurringEntityJpa entity = new CreditCardTransactionRecurringEntityJpa();
        applyFields(creditCardTransactionRecurring, entity);
        entity.setCreatedAt(creditCardTransactionRecurring.getCreatedDate());
        CreditCardTransactionRecurringEntityJpa saved = jpaCreditCardTransactionRecurringRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public CreditCardTransactionRecurring update(CreditCardTransactionRecurring creditCardTransactionRecurring) {
        CreditCardTransactionRecurringEntityJpa entity = jpaCreditCardTransactionRecurringRepository
                .findById(creditCardTransactionRecurring.getId()).orElseThrow();
        applyFields(creditCardTransactionRecurring, entity);
        CreditCardTransactionRecurringEntityJpa updated = jpaCreditCardTransactionRecurringRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public CreditCardTransactionRecurring findById(Long id) {
        return jpaCreditCardTransactionRecurringRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<CreditCardTransactionRecurring> findBySpaceId(Long spaceId) {
        Specification<CreditCardTransactionRecurringEntityJpa> specification = (root, query, criteriaBuilder) ->
                root.get("creditCard").get("id").in(creditCardIdsInSpace(spaceId, query, criteriaBuilder));
        return jpaCreditCardTransactionRecurringRepository.findAll(specification).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CreditCardTransactionRecurring> findByCreditCardId(Long creditCardId) {
        return jpaCreditCardTransactionRecurringRepository.findByCreditCard_Id(creditCardId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaCreditCardTransactionRecurringRepository.deleteById(id);
    }

    private Subquery<Long> creditCardIdsInSpace(Long spaceId, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<CreditCardEntityJpa> creditCardRoot = subquery.from(CreditCardEntityJpa.class);
        subquery.select(creditCardRoot.get("id"));
        subquery.where(criteriaBuilder.equal(creditCardRoot.get("space").get("id"), spaceId));
        return subquery;
    }

    private void applyFields(CreditCardTransactionRecurring creditCardTransactionRecurring, CreditCardTransactionRecurringEntityJpa entity) {
        entity.setCreditCard(jpaCreditCardRepository.getReferenceById(creditCardTransactionRecurring.getCreditCard().getId()));
        entity.setUser(jpaUserRepository.getReferenceById(creditCardTransactionRecurring.getUser().getId()));
        entity.setCategory(jpaCategoryRepository.getReferenceById(creditCardTransactionRecurring.getCategory().getId()));
        entity.setSubCategory(creditCardTransactionRecurring.getSubCategory() != null
                ? jpaSubCategoryRepository.getReferenceById(creditCardTransactionRecurring.getSubCategory().getId()) : null);
        entity.setDescription(creditCardTransactionRecurring.getDescription());
        entity.setDefaultAmount(creditCardTransactionRecurring.getDefaultAmount());
        entity.setStartDate(creditCardTransactionRecurring.getStartDate());
        entity.setActive(creditCardTransactionRecurring.isActive());
    }

    private CreditCardTransactionRecurring toDomain(CreditCardTransactionRecurringEntityJpa entity) {
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
