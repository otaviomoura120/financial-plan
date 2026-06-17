package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceMemberRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaUserRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceMemberEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.UserEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class SpaceMemberRepositoryImpl implements SpaceMemberRepository {

    private final JpaSpaceMemberRepository jpaSpaceMemberRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;

    public SpaceMemberRepositoryImpl(JpaSpaceMemberRepository jpaSpaceMemberRepository,
                                     JpaSpaceRepository jpaSpaceRepository,
                                     JpaUserRepository jpaUserRepository,
                                     JpaRoleRepository jpaRoleRepository) {
        this.jpaSpaceMemberRepository = jpaSpaceMemberRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.jpaRoleRepository = jpaRoleRepository;
    }

    @Override
    public SpaceMember save(SpaceMember member) {
        SpaceMemberEntityJpa entity = new SpaceMemberEntityJpa();
        applyFields(member, entity);
        SpaceMemberEntityJpa saved = jpaSpaceMemberRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public SpaceMember update(SpaceMember member) {
        SpaceMemberEntityJpa entity = jpaSpaceMemberRepository.findById(member.getId()).orElseThrow();
        entity.setRole(jpaRoleRepository.getReferenceById(member.getRole().getId()));
        SpaceMemberEntityJpa updated = jpaSpaceMemberRepository.saveAndFlush(entity);
        return toDomain(updated);
    }

    @Override
    public SpaceMember findBySpaceIdAndUserId(Long spaceId, Long userId) {
        return jpaSpaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public List<SpaceMember> findBySpaceId(Long spaceId) {
        return jpaSpaceMemberRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SpaceMember> findByUserId(Long userId) {
        return jpaSpaceMemberRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaSpaceMemberRepository.deleteById(id);
    }

    private void applyFields(SpaceMember member, SpaceMemberEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(member.getSpace().getId()));
        entity.setUser(jpaUserRepository.getReferenceById(member.getUser().getId()));
        entity.setRole(jpaRoleRepository.getReferenceById(member.getRole().getId()));
        entity.setJoinedAt(member.getJoinedAt());
    }

    private SpaceMember toDomain(SpaceMemberEntityJpa entity) {
        Space space = buildSpace(entity.getSpace());
        User user = buildUser(entity.getUser());
        Role role = buildRole(entity.getRole());
        return new SpaceMember(entity.getId(), entity.getVersion(), space, user, role, entity.getJoinedAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private User buildUser(UserEntityJpa entity) {
        return new User(entity.getId(), entity.getVersion(), entity.getAuth0Sub(), entity.getName(),
                entity.getNickname(), entity.getProfilePhoto(), entity.getObservation(), entity.getBirthdate(),
                entity.getEmail(), entity.getPhoneNumber(), entity.isActive(), entity.getGenre(),
                entity.getMaritalStatus(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.isMasterAdmin());
    }

    private Role buildRole(RoleEntityJpa entity) {
        Space roleSpace = buildSpace(entity.getSpace());
        return new Role(entity.getId(), entity.getVersion(), roleSpace, entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
