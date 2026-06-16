package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryImpl(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntityJpa entity = new UserEntityJpa();
        applyFields(user, entity);
        UserEntityJpa saved = jpaUserRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public User update(User user) {
        UserEntityJpa entity = jpaUserRepository.findById(user.getId()).orElseThrow();
        applyFields(user, entity);
        UserEntityJpa updated = jpaUserRepository.save(entity);
        return toDomain(updated);
    }

    @Override
    public User findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public User findByAuth0Sub(String auth0Sub) {
        return jpaUserRepository.findByAuth0Sub(auth0Sub)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaUserRepository.deleteById(id);
    }

    private void applyFields(User user, UserEntityJpa entity) {
        entity.setAuth0Sub(user.getAuth0Sub());
        entity.setName(user.getName());
        entity.setNickname(user.getNickname());
        entity.setProfilePhoto(user.getProfilePhoto());
        entity.setObservation(user.getObservation());
        entity.setBirthdate(user.getBirthdate());
        entity.setEmail(user.getEmail());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setActive(user.isActive());
        entity.setGenre(user.getGenre());
        entity.setMaritalStatus(user.getMaritalStatus());
        entity.setCreatedAt(user.getCreatedDate());
        entity.setUpdatedAt(user.getUpdatedDate());
    }

    private User toDomain(UserEntityJpa entity) {
        return new User(entity.getId(), entity.getVersion(), entity.getAuth0Sub(), entity.getName(),
                entity.getNickname(), entity.getProfilePhoto(), entity.getObservation(), entity.getBirthdate(),
                entity.getEmail(), entity.getPhoneNumber(), entity.isActive(), entity.getGenre(),
                entity.getMaritalStatus(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.isMasterAdmin());
    }
}
