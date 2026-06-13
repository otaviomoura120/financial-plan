package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest;
import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateSpaceService {

    private final SpaceRepository spaceRepository;
    private final RoleRepository roleRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final UserRepository userRepository;

    public CreateSpaceService(SpaceRepository spaceRepository, RoleRepository roleRepository,
                              SpaceMemberRepository spaceMemberRepository, UserRepository userRepository) {
        this.spaceRepository = spaceRepository;
        this.roleRepository = roleRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.userRepository = userRepository;
    }

    public SpaceResponse execute(CreateSpaceRequest request) {
        User creator = userRepository.findById(request.creatorId());
        Space space = new Space(null, 0, request.name(), request.description(), Instant.now(), null);
        space.validate();
        Space saved = spaceRepository.save(space);
        createOwnerMembership(creator, saved);
        return new SpaceResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getCreatedDate());
    }

    private void createOwnerMembership(User creator, Space space) {
        Role ownerRole = new Role(null, 0, space, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null);
        Role savedRole = roleRepository.save(ownerRole);
        SpaceMember membership = new SpaceMember(null, space, creator, savedRole, Instant.now());
        membership.validate();
        spaceMemberRepository.save(membership);
    }
}
