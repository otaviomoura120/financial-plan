package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

class SpaceSpec extends Specification {

    def "a Space with #members members has room for one more"() {
        when:
        Space.requireRoomForNewMember(members)

        then:
        noExceptionThrown()

        where:
        members << [0, 1, Space.MAX_MEMBERS - 1]
    }

    def "a Space with #members members is full"() {
        when:
        Space.requireRoomForNewMember(members)

        then:
        DomainException exception = thrown()
        exception.message == "space_member_limit_reached"

        where:
        members << [Space.MAX_MEMBERS, Space.MAX_MEMBERS + 1]
    }

    def "the limit is five users, the owner included"() {
        expect:
        Space.MAX_MEMBERS == 5
    }
}
