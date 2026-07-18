package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBankAccountRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaCreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class CreditCardRepositoryImpl implements CreditCardRepository {

    private final JpaCreditCardRepository jpaCreditCardRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaBankAccountRepository jpaBankAccountRepository;

    public CreditCardRepositoryImpl(JpaCreditCardRepository jpaCreditCardRepository, JpaSpaceRepository jpaSpaceRepository,
                                    JpaBankAccountRepository jpaBankAccountRepository) {
        this.jpaCreditCardRepository = jpaCreditCardRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaBankAccountRepository = jpaBankAccountRepository;
    }

    @Override
    public CreditCard save(CreditCard creditCard) {
        CreditCardEntityJpa entity = new CreditCardEntityJpa();
        applyFields(creditCard, entity);
        CreditCardEntityJpa saved = jpaCreditCardRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public CreditCard update(CreditCard creditCard) {
        CreditCardEntityJpa entity = jpaCreditCardRepository.findById(creditCard.getId()).orElseThrow();
        entity.setName(creditCard.getName());
        entity.setLimit(creditCard.getLimit());
        entity.setClosingDay(creditCard.getClosingDay());
        entity.setDueDay(creditCard.getDueDay());
        entity.setActive(creditCard.isActive());
        entity.setBankAccount(bankAccountReference(creditCard));
        CreditCardEntityJpa updated = jpaCreditCardRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public CreditCard findById(Long id) {
        return jpaCreditCardRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<CreditCard> findBySpaceId(Long spaceId) {
        return jpaCreditCardRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaCreditCardRepository.deleteById(id);
    }

    private void applyFields(CreditCard creditCard, CreditCardEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(creditCard.getSpace().getId()));
        entity.setBankAccount(bankAccountReference(creditCard));
        entity.setName(creditCard.getName());
        entity.setLimit(creditCard.getLimit());
        entity.setClosingDay(creditCard.getClosingDay());
        entity.setDueDay(creditCard.getDueDay());
        entity.setActive(creditCard.isActive());
        entity.setCreatedAt(creditCard.getCreatedDate());
    }

    private BankAccountEntityJpa bankAccountReference(CreditCard creditCard) {
        if (creditCard.getBankAccount() == null) {
            return null;
        }
        return jpaBankAccountRepository.getReferenceById(creditCard.getBankAccount().getId());
    }

    private CreditCard toDomain(CreditCardEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        BankAccount bankAccount = entity.getBankAccount() != null ? buildBankAccount(entity.getBankAccount()) : null;
        return new CreditCard(entity.getId(), entity.getVersion(), space, bankAccount, entity.getName(), entity.getLimit(),
                entity.getClosingDay(), entity.getDueDay(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private BankAccount buildBankAccount(BankAccountEntityJpa entity) {
        return new BankAccount(entity.getId(), entity.getVersion(), null, entity.getName(),
                entity.getBankName(), entity.getBalance(), entity.isActive(), entity.getCreatedAt(), null);
    }
}
