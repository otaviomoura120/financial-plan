package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.enums.InviteStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

class SpaceInviteSpec extends Specification {

    private SpaceInvite buildInvite(InviteStatus status, Instant expiresAt) {
        Space space = new Space(1L, 0, "Família", null, Instant.now(), null)
        Role role = new Role(2L, 0, space, "MEMBER", null, Instant.now(), null)
        new SpaceInvite(3L, 0, space, role, "maria@example.com", "token", status, Instant.now(), expiresAt)
    }

    def "a pending invite that has not expired can be answered"() {
        given:
        SpaceInvite invite = buildInvite(InviteStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS))

        when:
        invite.requireAnswerable()

        then:
        noExceptionThrown()
    }

    def "an invite in status #status cannot be answered and reports '#code'"() {
        given:
        SpaceInvite invite = buildInvite(status, Instant.now().plus(1, ChronoUnit.DAYS))

        when:
        invite.requireAnswerable()

        then:
        DomainException exception = thrown()
        exception.message == code

        where:
        status                 | code
        InviteStatus.CANCELLED | "invite_cancelled"
        InviteStatus.ACCEPTED  | "invite_already_accepted"
        InviteStatus.DECLINED  | "invite_already_declined"
    }

    def "an expired pending invite cannot be answered"() {
        given:
        SpaceInvite invite = buildInvite(InviteStatus.PENDING, Instant.now().minus(1, ChronoUnit.MINUTES))

        when:
        invite.requireAnswerable()

        then:
        DomainException exception = thrown()
        exception.message == "invite_expired"
    }

    def "the status check comes before the expiry check"() {
        given:
        SpaceInvite invite = buildInvite(InviteStatus.DECLINED, Instant.now().minus(1, ChronoUnit.DAYS))

        when:
        invite.requireAnswerable()

        then:
        DomainException exception = thrown()
        exception.message == "invite_already_declined"
    }

    def "#members members and #pending pending invites leave room for another invite"() {
        given:
        List<SpaceInvite> invites = (0..<pending).collect { buildInvite(InviteStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS)) }

        when:
        SpaceInvite.requireRoomForNewInvite(members, invites)

        then:
        noExceptionThrown()

        where:
        members | pending
        1       | 0
        1       | 3
        4       | 0
        2       | 2
    }

    def "#members members and #pending pending invites fill the Space"() {
        given:
        List<SpaceInvite> invites = (0..<pending).collect { buildInvite(InviteStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS)) }

        when:
        SpaceInvite.requireRoomForNewInvite(members, invites)

        then:
        DomainException exception = thrown()
        exception.message == "space_member_limit_reached"

        where:
        members | pending
        1       | 4
        5       | 0
        3       | 2
        6       | 0
    }

    def "expired and answered invites do not hold a seat"() {
        given:
        List<SpaceInvite> invites = [
                buildInvite(InviteStatus.PENDING, Instant.now().minus(1, ChronoUnit.DAYS)),
                buildInvite(InviteStatus.ACCEPTED, Instant.now().plus(1, ChronoUnit.DAYS)),
                buildInvite(InviteStatus.CANCELLED, Instant.now().plus(1, ChronoUnit.DAYS)),
                buildInvite(InviteStatus.DECLINED, Instant.now().plus(1, ChronoUnit.DAYS))
        ]

        when:
        SpaceInvite.requireRoomForNewInvite(4, invites)

        then:
        noExceptionThrown()
    }

    def "only pending invites that have not expired count as pending"() {
        expect:
        buildInvite(status, Instant.now().plus(daysUntilExpiry, ChronoUnit.DAYS)).isPending() == pending

        where:
        status                 | daysUntilExpiry | pending
        InviteStatus.PENDING   | 1               | true
        InviteStatus.PENDING   | -1              | false
        InviteStatus.ACCEPTED  | 1               | false
        InviteStatus.CANCELLED | 1               | false
        InviteStatus.DECLINED  | 1               | false
    }
}
