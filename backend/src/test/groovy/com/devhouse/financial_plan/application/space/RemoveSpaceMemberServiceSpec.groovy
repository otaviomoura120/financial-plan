package com.devhouse.financial_plan.application.space

import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import spock.lang.Specification

import java.time.Instant

class RemoveSpaceMemberServiceSpec extends Specification {

    SpaceMemberRepository spaceMemberRepository = Mock()
    RemoveSpaceMemberService service = new RemoveSpaceMemberService(spaceMemberRepository)

    def "execute removes member who does not have OWNER role"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        User user = new User(5L, 0, "auth0|abc", "Jane", null, null, null,
                null, "jane@test.com", null, true, null, null, Instant.now(), null, false)
        Role memberRole = new Role(2L, 0, space, "MEMBER", "Regular member", Instant.now(), null)
        SpaceMember member = new SpaceMember(10L, space, user, memberRole, Instant.now())
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 5L) >> member

        when:
        service.execute(1L, 5L)

        then:
        1 * spaceMemberRepository.delete(10L)
    }

    def "execute throws DomainException when trying to remove the OWNER"() {
        given:
        Space space = new Space(1L, 0, "My Space", null, Instant.now(), null)
        User owner = new User(2L, 0, "auth0|abc", "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null, false)
        Role ownerRole = new Role(1L, 0, space, Role.OWNER_ROLE_NAME, "Space owner", Instant.now(), null)
        SpaceMember ownerMember = new SpaceMember(9L, space, owner, ownerRole, Instant.now())
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 2L) >> ownerMember

        when:
        service.execute(1L, 2L)

        then:
        thrown(DomainException)
        0 * spaceMemberRepository.delete(_)
    }

    def "execute throws DomainException when user is not a member of the space"() {
        given:
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 5L) >> null

        when:
        service.execute(1L, 5L)

        then:
        thrown(DomainException)
        0 * spaceMemberRepository.delete(_)
    }
}
