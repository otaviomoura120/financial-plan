package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.PaymentMethod;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.PaymentMethodRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaPaymentMethodRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.PaymentMethodEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class PaymentMethodRepositoryImpl implements PaymentMethodRepository {

    private final JpaPaymentMethodRepository jpaPaymentMethodRepository;
    private final JpaSpaceRepository jpaSpaceRepository;

    public PaymentMethodRepositoryImpl(JpaPaymentMethodRepository jpaPaymentMethodRepository, JpaSpaceRepository jpaSpaceRepository) {
        this.jpaPaymentMethodRepository = jpaPaymentMethodRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
    }

    @Override
    public PaymentMethod save(PaymentMethod paymentMethod) {
        PaymentMethodEntityJpa entity = new PaymentMethodEntityJpa();
        applyFields(paymentMethod, entity);
        PaymentMethodEntityJpa saved = jpaPaymentMethodRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public PaymentMethod update(PaymentMethod paymentMethod) {
        PaymentMethodEntityJpa entity = jpaPaymentMethodRepository.findById(paymentMethod.getId()).orElseThrow();
        entity.setName(paymentMethod.getName());
        entity.setActive(paymentMethod.isActive());
        entity.setUpdatedAt(paymentMethod.getUpdatedDate());
        PaymentMethodEntityJpa updated = jpaPaymentMethodRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public PaymentMethod findById(Long id) {
        return jpaPaymentMethodRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<PaymentMethod> findBySpaceId(Long spaceId) {
        return jpaPaymentMethodRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaPaymentMethodRepository.deleteById(id);
    }

    private void applyFields(PaymentMethod paymentMethod, PaymentMethodEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(paymentMethod.getSpace().getId()));
        entity.setName(paymentMethod.getName());
        entity.setActive(paymentMethod.isActive());
        entity.setCreatedAt(paymentMethod.getCreatedDate());
        entity.setUpdatedAt(paymentMethod.getUpdatedDate());
    }

    private PaymentMethod toDomain(PaymentMethodEntityJpa entity) {
        Space space = entity.getSpace() != null ? buildSpace(entity.getSpace()) : null;
        return new PaymentMethod(entity.getId(), null, space, entity.getName(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
