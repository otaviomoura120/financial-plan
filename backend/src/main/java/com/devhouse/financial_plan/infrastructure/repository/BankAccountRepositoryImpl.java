package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.BankAccount;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.BankAccountRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.BankAccountEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaBankAccountRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final JpaBankAccountRepository jpaBankAccountRepository;
    private final JpaSpaceRepository jpaSpaceRepository;

    public BankAccountRepositoryImpl(JpaBankAccountRepository jpaBankAccountRepository, JpaSpaceRepository jpaSpaceRepository) {
        this.jpaBankAccountRepository = jpaBankAccountRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
    }

    @Override
    public BankAccount save(BankAccount bankAccount) {
        BankAccountEntityJpa entity = new BankAccountEntityJpa();
        applyFields(bankAccount, entity);
        BankAccountEntityJpa saved = jpaBankAccountRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public BankAccount update(BankAccount bankAccount) {
        BankAccountEntityJpa entity = jpaBankAccountRepository.findById(bankAccount.getId()).orElseThrow();
        entity.setName(bankAccount.getName());
        entity.setBankName(bankAccount.getBankName());
        entity.setBalance(bankAccount.getBalance());
        entity.setActive(bankAccount.isActive());
        BankAccountEntityJpa updated = jpaBankAccountRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public BankAccount findById(Long id) {
        return jpaBankAccountRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<BankAccount> findBySpaceId(Long spaceId) {
        return jpaBankAccountRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaBankAccountRepository.deleteById(id);
    }

    private void applyFields(BankAccount bankAccount, BankAccountEntityJpa entity) {
        entity.setSpaceId(bankAccount.getSpace().getId());
        entity.setName(bankAccount.getName());
        entity.setBankName(bankAccount.getBankName());
        entity.setBalance(bankAccount.getBalance());
        entity.setActive(bankAccount.isActive());
        entity.setCreatedAt(bankAccount.getCreatedDate());
    }

    private BankAccount toDomain(BankAccountEntityJpa entity) {
        Space space = resolveSpace(entity.getSpaceId());
        return new BankAccount(entity.getId(), null, space, entity.getName(),
                entity.getBankName(), entity.getBalance(), entity.isActive(), entity.getCreatedAt(), null);
    }

    private Space resolveSpace(Long spaceId) {
        if (spaceId == null) {
            return null;
        }
        SpaceEntityJpa spaceEntity = jpaSpaceRepository.findById(spaceId).orElse(null);
        if (spaceEntity == null) {
            return null;
        }
        return new Space(spaceEntity.getId(), spaceEntity.getVersion(), spaceEntity.getName(),
                spaceEntity.getDescription(), spaceEntity.getCreatedAt(), spaceEntity.getUpdatedAt());
    }
}
