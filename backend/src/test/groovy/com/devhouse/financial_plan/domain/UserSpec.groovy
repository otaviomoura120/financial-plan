package com.devhouse.financial_plan.domain

import spock.lang.Specification

import java.time.Instant

class UserSpec extends Specification {

    private User buildUser(boolean active) {
        new User(10L, 0, "auth0|abc123", "John Smith", null, null, null, null,
                "john@example.com", null, active, null, null, Instant.now(), null, false)
    }

    def "deactivate sets active to false"() {
        given:
        User user = buildUser(true)

        when:
        user.deactivate()

        then:
        !user.isActive()
    }

    def "activate sets active to true"() {
        given:
        User user = buildUser(false)

        when:
        user.activate()

        then:
        user.isActive()
    }
}
