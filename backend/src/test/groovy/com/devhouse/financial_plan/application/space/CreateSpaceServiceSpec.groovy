package com.devhouse.financial_plan.application.space

import com.devhouse.financial_plan.application.role.CreateRoleService
import com.devhouse.financial_plan.application.role.dto.CreateRoleRequest
import com.devhouse.financial_plan.application.role.dto.RoleResponse
import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest
import com.devhouse.financial_plan.application.space.dto.SpaceResponse
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class CreateSpaceServiceSpec extends Specification {

    SpaceRepository spaceRepository = Mock()
    CreateRoleService createRoleService = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    UserRepository userRepository = Mock()
    CreateSpaceService service = new CreateSpaceService(spaceRepository, createRoleService, spaceMemberRepository, userRepository)

    def "execute creates space, delegates OWNER role creation to CreateRoleService, and saves membership"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("Smith Family", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
        Space savedSpace = new Space(10L, 0, "Smith Family", null, Instant.now(), null)
        RoleResponse ownerRoleResponse = new RoleResponse(20L, 0, 10L, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null)
        SpaceMember savedMember = new SpaceMember(30L, savedSpace, creator,
                new Role(20L, 0, savedSpace, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null), Instant.now())

        userRepository.findById(1L) >> creator
        spaceRepository.save(_) >> savedSpace
        spaceMemberRepository.save(_) >> savedMember

        when:
        SpaceResponse response = service.execute(request)

        then:
        1 * createRoleService.execute({ CreateRoleRequest r ->
            r.spaceId() == 10L && r.name() == Role.OWNER_ROLE_NAME
        }) >> ownerRoleResponse
        1 * spaceMemberRepository.save({ SpaceMember m ->
            m.getSpace().getId() == 10L && m.getUser().getId() == 1L && m.getRole().getId() == 20L
        }) >> savedMember
        response.id() == 10L
        response.name() == "Smith Family"
    }

    def "execute throws DomainException when space name is blank"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
        userRepository.findById(1L) >> creator

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * spaceRepository.save(_)
        0 * createRoleService.execute(_)
        0 * spaceMemberRepository.save(_)
    }
}
