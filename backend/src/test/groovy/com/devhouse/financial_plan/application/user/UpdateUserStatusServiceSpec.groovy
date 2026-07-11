package com.devhouse.financial_plan.application.user

import com.devhouse.financial_plan.application.user.dto.UserResponse
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class UpdateUserStatusServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    UpdateUserStatusService service = new UpdateUserStatusService(userRepository)

    private User buildUser(boolean active) {
        new User(10L, 0, "auth0|abc123", "John Smith", null, null, null, null,
                "john@example.com", null, active, null, null, Instant.now(), null, false)
    }

    def "execute activates an inactive user"() {
        given:
        User user = buildUser(false)
        userRepository.findById(10L) >> user

        when:
        UserResponse response = service.execute(10L, true)

        then:
        response.id() == 10L
        1 * userRepository.update({ it.isActive() }) >> { User u -> u }
    }

    def "execute deactivates an active user"() {
        given:
        User user = buildUser(true)
        userRepository.findById(10L) >> user

        when:
        UserResponse response = service.execute(10L, false)

        then:
        response.id() == 10L
        1 * userRepository.update({ !it.isActive() }) >> { User u -> u }
    }
}
