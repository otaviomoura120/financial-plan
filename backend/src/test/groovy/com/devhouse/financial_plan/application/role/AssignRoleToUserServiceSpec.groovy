package com.devhouse.financial_plan.application.role

import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.exception.NotFoundException
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import spock.lang.Specification

import java.time.Instant

class AssignRoleToUserServiceSpec extends Specification {

    SpaceMemberRepository spaceMemberRepository = Mock()
    RoleRepository roleRepository = Mock()
    AssignRoleToUserService service = new AssignRoleToUserService(spaceMemberRepository, roleRepository)

    Space space = new Space(1L, 0, "Família", null, Instant.now(), null)
    Role memberRole = new Role(2L, 0, space, "MEMBER", null, Instant.now(), null)
    User user = new User(7L, 0, "auth0|maria", "Maria", null, null, null, null, "maria@example.com", null, true,
            null, null, Instant.now(), null, false)

    def setup() {
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 7L) >> new SpaceMember(4L, 0, space, user, memberRole, Instant.now())
    }

    def "assigns a role of the same space to the member"() {
        given:
        roleRepository.findById(3L) >> new Role(3L, 0, space, "ADMIN", null, Instant.now(), null)

        when:
        service.execute(7L, 3L, 1L)

        then:
        1 * spaceMemberRepository.update({ SpaceMember member -> member.role.id == 3L })
    }

    def "responds not found when the role does not exist"() {
        given:
        roleRepository.findById(99L) >> null

        when:
        service.execute(7L, 99L, 1L)

        then:
        thrown(NotFoundException)
        0 * spaceMemberRepository.update(_)
    }

    def "responds not found when the role belongs to another space"() {
        given:
        Space otherSpace = new Space(5L, 0, "Outro", null, Instant.now(), null)
        roleRepository.findById(3L) >> new Role(3L, 0, otherSpace, "ADMIN", null, Instant.now(), null)

        when:
        service.execute(7L, 3L, 1L)

        then:
        thrown(NotFoundException)
        0 * spaceMemberRepository.update(_)
    }

    def "rejects a user who is not a member of the space"() {
        when:
        service.execute(8L, 3L, 1L)

        then:
        thrown(DomainException)
        0 * roleRepository.findById(_)
        0 * spaceMemberRepository.update(_)
    }
}
