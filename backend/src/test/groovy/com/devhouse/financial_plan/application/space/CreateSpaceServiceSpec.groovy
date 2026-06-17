package com.devhouse.financial_plan.application.space

import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest
import com.devhouse.financial_plan.application.space.dto.SpaceResponse
import com.devhouse.financial_plan.domain.EndpointPermission
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.EndpointPermissionAccess
import com.devhouse.financial_plan.domain.enums.EndpointPermissionType
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.EndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.RoleEndpointPermissionRepository
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class CreateSpaceServiceSpec extends Specification {

    SpaceRepository spaceRepository = Mock()
    RoleRepository roleRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    UserRepository userRepository = Mock()
    EndpointPermissionRepository endpointPermissionRepository = Mock()
    RoleEndpointPermissionRepository roleEndpointPermissionRepository = Mock()
    CreateSpaceService service = new CreateSpaceService(spaceRepository, roleRepository, spaceMemberRepository,
            userRepository, endpointPermissionRepository, roleEndpointPermissionRepository)

    def "execute creates space, owner role, assigns all non-internal permissions as ALLOW, and saves membership"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("Smith Family", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, false)
        Space savedSpace = new Space(10L, 0, "Smith Family", null, Instant.now(), null)
        Role savedOwnerRole = new Role(20L, 0, savedSpace, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null)
        EndpointPermission ep = new EndpointPermission(1L, 0, "/roles", "Roles", null, 1,
                EndpointPermissionType.API, "GET", "Role", Instant.now(), null)
        SpaceMember savedMember = new SpaceMember(30L, 0, savedSpace, creator, savedOwnerRole, Instant.now())

        userRepository.findById(1L) >> creator
        spaceRepository.save(_) >> savedSpace
        roleRepository.save({ Role r -> r.getName() == Role.OWNER_ROLE_NAME }) >> savedOwnerRole
        endpointPermissionRepository.findAll() >> [ep]
        spaceMemberRepository.save(_) >> savedMember

        when:
        SpaceResponse response = service.execute(request)

        then:
        1 * roleEndpointPermissionRepository.saveAll({ List reps ->
            reps.size() == 1 && reps[0].getPermission() == EndpointPermissionAccess.ALLOW
        })
        1 * spaceMemberRepository.save({ SpaceMember m ->
            m.getSpace().getId() == 10L && m.getUser().getId() == 1L && m.getRole().getId() == 20L
        }) >> savedMember
        response.id() == 10L
        response.name() == "Smith Family"
    }

    def "execute excludes internal_management permissions when assigning ALLOW to OWNER role"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("Family", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, false)
        Space savedSpace = new Space(10L, 0, "Family", null, Instant.now(), null)
        Role savedOwnerRole = new Role(20L, 0, savedSpace, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null)
        EndpointPermission normalEp = new EndpointPermission(1L, 0, "/roles", "Roles", null, 1,
                EndpointPermissionType.API, "GET", "Role", Instant.now(), null)
        EndpointPermission internalEp = new EndpointPermission(2L, 0, "/endpoint-permissions", "Permissions", null, 1,
                EndpointPermissionType.API, "GET", EndpointPermission.INTERNAL_MANAGEMENT_GROUP, Instant.now(), null)

        userRepository.findById(1L) >> creator
        spaceRepository.save(_) >> savedSpace
        roleRepository.save(_) >> savedOwnerRole
        endpointPermissionRepository.findAll() >> [normalEp, internalEp]
        spaceMemberRepository.save(_) >> new SpaceMember(30L, 0, savedSpace, creator, savedOwnerRole, Instant.now())

        when:
        service.execute(request)

        then:
        1 * roleEndpointPermissionRepository.saveAll({ List reps ->
            reps.size() == 1 && reps[0].getEndpointPermission().getId() == 1L
        })
    }

    def "execute throws DomainException when space name is blank"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, false)
        userRepository.findById(1L) >> creator

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * spaceRepository.save(_)
        0 * roleRepository.save(_)
        0 * roleEndpointPermissionRepository.saveAll(_)
        0 * spaceMemberRepository.save(_)
    }
}
