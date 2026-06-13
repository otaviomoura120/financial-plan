package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaFamilyRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final JpaFamilyRepository jpaFamilyRepository;
    private final JpaRoleRepository jpaRoleRepository;

    public UserRepositoryImpl(JpaUserRepository jpaUserRepository,
                               JpaFamilyRepository jpaFamilyRepository,
                               JpaRoleRepository jpaRoleRepository) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaFamilyRepository = jpaFamilyRepository;
        this.jpaRoleRepository = jpaRoleRepository;
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
        applyUpdatableFields(user, entity);
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
        if (user.getFamily() != null) {
            entity.setFamily(jpaFamilyRepository.getReferenceById(user.getFamily().getId()));
        }
        if (user.getRole() != null) {
            entity.setRole(jpaRoleRepository.getReferenceById(user.getRole().getId()));
        }
    }

    private void applyUpdatableFields(User user, UserEntityJpa entity) {
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
        entity.setUpdatedAt(user.getUpdatedDate());
        if (user.getFamily() != null) {
            entity.setFamily(jpaFamilyRepository.getReferenceById(user.getFamily().getId()));
        }
        if (user.getRole() != null) {
            entity.setRole(jpaRoleRepository.getReferenceById(user.getRole().getId()));
        } else {
            entity.setRole(null);
        }
    }

    private User toDomain(UserEntityJpa entity) {
        Family family = buildFamily(entity);
        Role role = buildRole(entity);
        return new User(entity.getId(), entity.getVersion(), family, entity.getAuth0Sub(), role,
                entity.getName(), entity.getNickname(), entity.getProfilePhoto(), entity.getObservation(),
                entity.getBirthdate(), entity.getEmail(), entity.getPhoneNumber(), entity.isActive(),
                entity.getGenre(), entity.getMaritalStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Family buildFamily(UserEntityJpa entity) {
        if (entity.getFamily() == null) {
            return null;
        }
        return new Family(entity.getFamily().getId(), null, entity.getFamily().getName(), entity.getFamily().getCreatedAt(), null);
    }

    private Role buildRole(UserEntityJpa entity) {
        if (entity.getRole() == null) {
            return null;
        }
        RoleEntityJpa roleEntity = entity.getRole();
        Family roleFamily = new Family(roleEntity.getFamily().getId(), null, roleEntity.getFamily().getName(), roleEntity.getFamily().getCreatedAt(), null);
        return new Role(roleEntity.getId(), roleEntity.getVersion(), roleFamily, roleEntity.getName(), roleEntity.getDescription(), roleEntity.getCreatedAt(), roleEntity.getUpdatedAt());
    }
}
