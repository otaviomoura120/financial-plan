package com.devhouse.financial_plan.infrastructure.repository;

import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.enums.InviteStatus;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceInviteRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaRoleRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.JpaSpaceRepository;
import com.devhouse.financial_plan.infrastructure.repository.jpa.RoleEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceEntityJpa;
import com.devhouse.financial_plan.infrastructure.repository.jpa.SpaceInviteEntityJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class SpaceInviteRepositoryImpl implements SpaceInviteRepository {

    private final JpaSpaceInviteRepository jpaSpaceInviteRepository;
    private final JpaSpaceRepository jpaSpaceRepository;
    private final JpaRoleRepository jpaRoleRepository;

    public SpaceInviteRepositoryImpl(JpaSpaceInviteRepository jpaSpaceInviteRepository,
                                     JpaSpaceRepository jpaSpaceRepository,
                                     JpaRoleRepository jpaRoleRepository) {
        this.jpaSpaceInviteRepository = jpaSpaceInviteRepository;
        this.jpaSpaceRepository = jpaSpaceRepository;
        this.jpaRoleRepository = jpaRoleRepository;
    }

    @Override
    public SpaceInvite save(SpaceInvite invite) {
        SpaceInviteEntityJpa entity = new SpaceInviteEntityJpa();
        applyFields(invite, entity);
        return toDomain(jpaSpaceInviteRepository.save(entity));
    }

    @Override
    public SpaceInvite update(SpaceInvite invite) {
        SpaceInviteEntityJpa entity = jpaSpaceInviteRepository.findById(invite.getId()).orElseThrow();
        entity.setStatus(invite.getStatus());
        return toDomain(jpaSpaceInviteRepository.save(entity));
    }

    @Override
    public Optional<SpaceInvite> findByToken(String token) {
        return jpaSpaceInviteRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public List<SpaceInvite> findBySpaceId(Long spaceId) {
        return jpaSpaceInviteRepository.findBySpaceId(spaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SpaceInvite> findBySpaceIdAndEmail(Long spaceId, String email) {
        return jpaSpaceInviteRepository.findBySpaceIdAndEmail(spaceId, email).map(this::toDomain);
    }

    @Override
    public List<SpaceInvite> findByEmailIgnoreCaseAndStatus(String email, InviteStatus status) {
        return jpaSpaceInviteRepository.findByEmailIgnoreCaseAndStatus(email, status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        jpaSpaceInviteRepository.deleteById(id);
    }

    private void applyFields(SpaceInvite invite, SpaceInviteEntityJpa entity) {
        entity.setSpace(jpaSpaceRepository.getReferenceById(invite.getSpace().getId()));
        entity.setRole(jpaRoleRepository.getReferenceById(invite.getRole().getId()));
        entity.setEmail(invite.getEmail());
        entity.setToken(invite.getToken());
        entity.setStatus(invite.getStatus());
        entity.setCreatedAt(invite.getCreatedAt());
        entity.setExpiresAt(invite.getExpiresAt());
    }

    private SpaceInvite toDomain(SpaceInviteEntityJpa entity) {
        Space space = buildSpace(entity.getSpace());
        Role role = buildRole(entity.getRole());
        return new SpaceInvite(entity.getId(), entity.getVersion(), space, role, entity.getEmail(),
                entity.getToken(), entity.getStatus(), entity.getCreatedAt(), entity.getExpiresAt());
    }

    private Space buildSpace(SpaceEntityJpa entity) {
        return new Space(entity.getId(), entity.getVersion(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private Role buildRole(RoleEntityJpa entity) {
        Space roleSpace = buildSpace(entity.getSpace());
        return new Role(entity.getId(), entity.getVersion(), roleSpace, entity.getName(),
                entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
