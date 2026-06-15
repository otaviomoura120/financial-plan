package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.role.CreateRoleService;
import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest;
import com.devhouse.financial_plan.application.role.dto.RoleResponse;
import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest;
import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateSpaceService {

    private final SpaceRepository spaceRepository;
    private final CreateRoleService createRoleService;
    private final SpaceMemberRepository spaceMemberRepository;
    private final UserRepository userRepository;

    public CreateSpaceService(SpaceRepository spaceRepository, CreateRoleService createRoleService,
                              SpaceMemberRepository spaceMemberRepository, UserRepository userRepository) {
        this.spaceRepository = spaceRepository;
        this.createRoleService = createRoleService;
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
        RoleResponse roleResponse = createRoleService.execute(
                new CreateRoleRequest(space.getId(), Role.OWNER_ROLE_NAME, "Space owner"));
        Role ownerRole = new Role(roleResponse.id(), roleResponse.version(), space,
                roleResponse.name(), roleResponse.description(), roleResponse.createdAt(), roleResponse.updatedAt());
        SpaceMember membership = new SpaceMember(null, space, creator, ownerRole, Instant.now());
        membership.validate();
        spaceMemberRepository.save(membership);
    }
}
