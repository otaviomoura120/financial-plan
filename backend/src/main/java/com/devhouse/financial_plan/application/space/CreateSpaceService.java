package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest;
import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.domain.EndpointPermission;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.RoleEndpointPermission;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess;
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CreateSpaceService {

    private final SpaceRepository spaceRepository;
    private final RoleRepository roleRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final UserRepository userRepository;
    private final EndpointPermissionRepository endpointPermissionRepository;
    private final RoleEndpointPermissionRepository roleEndpointPermissionRepository;

    public CreateSpaceService(SpaceRepository spaceRepository, RoleRepository roleRepository,
                              SpaceMemberRepository spaceMemberRepository, UserRepository userRepository,
                              EndpointPermissionRepository endpointPermissionRepository,
                              RoleEndpointPermissionRepository roleEndpointPermissionRepository) {
        this.spaceRepository = spaceRepository;
        this.roleRepository = roleRepository;
        this.spaceMemberRepository = spaceMemberRepository;
        this.userRepository = userRepository;
        this.endpointPermissionRepository = endpointPermissionRepository;
        this.roleEndpointPermissionRepository = roleEndpointPermissionRepository;
    }

    public SpaceResponse execute(CreateSpaceRequest request) {
        User creator = userRepository.findById(request.creatorId());
        Space space = new Space(null, 0, request.name(), request.description(), Instant.now(), null);
        space.validate();
        Space saved = spaceRepository.save(space);
        createOwnerMembership(creator, saved);
        return new SpaceResponse(saved.getId(), saved.getVersion(), saved.getName(), saved.getDescription(), saved.getCreatedDate(), null);
    }

    private void createOwnerMembership(User creator, Space space) {
        // Create OWNER role directly — bypassing CreateRoleService so its createDefaultPermissions
        // does not insert DENY entries that would conflict with the ALLOW entries we add below.
        Role ownerRole = new Role(null, 0, space, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null);
        ownerRole.validate();
        Role savedOwnerRole = roleRepository.save(ownerRole);

        List<EndpointPermission> permissions = endpointPermissionRepository.findAll().stream()
                .filter(ep -> !ep.isInternalManagement())
                .toList();
        List<RoleEndpointPermission> rolePermissions = permissions.stream()
                .map(ep -> new RoleEndpointPermission(null, 0, savedOwnerRole, ep,
                        EndpointPermissionAccess.ALLOW, Instant.now(), Instant.now()))
                .toList();
        roleEndpointPermissionRepository.saveAll(rolePermissions);

        SpaceMember membership = new SpaceMember(null, null, space, creator, savedOwnerRole, Instant.now());
        membership.validate();
        spaceMemberRepository.save(membership);
    }
}
