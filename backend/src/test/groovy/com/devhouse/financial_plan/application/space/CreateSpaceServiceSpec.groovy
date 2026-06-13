package com.devhouse.financial_plan.application.space

import com.devhouse.financial_plan.application.space.dto.CreateSpaceRequest
import com.devhouse.financial_plan.application.space.dto.SpaceResponse
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
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
    CreateSpaceService service = new CreateSpaceService(spaceRepository, roleRepository, spaceMemberRepository, userRepository)

    def "execute creates space, OWNER role, and space member for creator"() {
        given:
        CreateSpaceRequest request = new CreateSpaceRequest("Smith Family", null, 1L)
        User creator = new User(1L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
        Space savedSpace = new Space(10L, 0, "Smith Family", null, Instant.now(), null)
        Role savedRole = new Role(20L, 0, savedSpace, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null)
        SpaceMember savedMember = new SpaceMember(30L, savedSpace, creator, savedRole, Instant.now())

        userRepository.findById(1L) >> creator
        spaceRepository.save(_) >> savedSpace
        roleRepository.save(_) >> savedRole
        spaceMemberRepository.save(_) >> savedMember

        when:
        SpaceResponse response = service.execute(request)

        then:
        1 * roleRepository.save({ Role r -> r.getName() == Role.OWNER_ROLE_NAME && r.getSpace().getId() == 10L }) >> savedRole
        1 * spaceMemberRepository.save({ SpaceMember m -> m.getSpace().getId() == 10L && m.getUser().getId() == 1L }) >> savedMember
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
        0 * roleRepository.save(_)
        0 * spaceMemberRepository.save(_)
    }
}
