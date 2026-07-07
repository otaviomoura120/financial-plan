package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.CreditCard;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.CreditCardRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.CreditCardEntityJpa;
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

    public CreditCardRepositoryImpl(JpaCreditCardRepository jpaCreditCardRepository, JpaSpaceRepository jpaSpaceRepository) {
        this.jpaCreditCardRepository = jpaCreditCardRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
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
        entity.setName(creditCard.getName());
        entity.setLimit(creditCard.getLimit());
        entity.setClosingDay(creditCard.getClosingDay());
        entity.setDueDay(creditCard.getDueDay());
        entity.setActive(creditCard.isActive());
        entity.setCreatedAt(creditCard.getCreatedDate());
    }

    private CreditCard toDomain(CreditCardEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new CreditCard(entity.getId(), entity.getVersion(), space, entity.getName(), entity.getLimit(),
                entity.getClosingDay(), entity.getDueDay(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
