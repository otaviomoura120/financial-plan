package com.devhouse.financial_plan.application.invite

import com.devhouse.financial_plan.application.invite.dto.AcceptInviteResponse
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceInvite
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.InviteStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

class AcceptInviteServiceSpec extends Specification {

    SpaceInviteRepository spaceInviteRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    UserRepository userRepository = Mock()
    AcceptInviteService service = new AcceptInviteService(spaceInviteRepository, spaceMemberRepository, userRepository)

    Space space = new Space(1L, 0, "Família", null, Instant.now(), null)
    Role role = new Role(2L, 0, space, "MEMBER", null, Instant.now(), null)

    private SpaceInvite givenInvite(InviteStatus status) {
        SpaceInvite invite = new SpaceInvite(3L, 0, space, role, "maria@example.com", "token", status,
                Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS))
        spaceInviteRepository.findByToken("token") >> Optional.of(invite)
        return invite
    }

    private void givenUser(String email) {
        userRepository.findByAuth0Sub("auth0|maria") >> new User(7L, 0, "auth0|maria", "Maria", null, null, null, null,
                email, null, true, null, null, Instant.now(), null, false)
    }

    def "accepts a pending invite, adds the member with the invite role and marks the invite accepted"() {
        given:
        SpaceInvite invite = givenInvite(InviteStatus.PENDING)
        givenUser("Maria@Example.com")
        spaceMemberRepository.findBySpaceIdAndUserId(1L, 7L) >> null

        when:
        AcceptInviteResponse result = service.execute("token", "auth0|maria")

        then:
        1 * spaceMemberRepository.save({ SpaceMember member -> member.user.id == 7L && member.role.id == 2L && member.space.id == 1L })
        1 * spaceInviteRepository.update({ SpaceInvite updated -> updated.status == InviteStatus.ACCEPTED })
        result.spaceId() == 1L
        result.roleId() == 2L
        invite.status == InviteStatus.ACCEPTED
    }

    def "does not accept an invite the recipient already declined"() {
        given:
        givenInvite(InviteStatus.DECLINED)
        givenUser("maria@example.com")

        when:
        service.execute("token", "auth0|maria")

        then:
        DomainException exception = thrown()
        exception.message == "invite_already_declined"
        0 * spaceMemberRepository.save(_)
        0 * spaceInviteRepository.update(_)
    }

    def "does not accept an invite addressed to another e-mail"() {
        given:
        givenInvite(InviteStatus.PENDING)
        givenUser("joao@example.com")

        when:
        service.execute("token", "auth0|maria")

        then:
        DomainException exception = thrown()
        exception.message == "invite_email_mismatch"
        0 * spaceMemberRepository.save(_)
    }
}
