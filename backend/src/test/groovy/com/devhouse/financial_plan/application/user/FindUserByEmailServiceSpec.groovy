package com.devhouse.financial_plan.application.user

import com.devhouse.financial_plan.application.user.dto.UserSearchResponse
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class FindUserByEmailServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    FindUserByEmailService service = new FindUserByEmailService(userRepository)

    def "execute returns only the masked name of the user found by e-mail"() {
        given:
        userRepository.findByEmail("joao@example.com") >> Optional.of(new User(7L, 0, "auth0|7", "João Silva", null, null,
                null, null, "joao@example.com", null, true, null, null, Instant.now(), null, false))

        when:
        Optional<UserSearchResponse> response = service.execute("joao@example.com")

        then:
        response.get() == new UserSearchResponse("Joã* S****")
    }

    def "execute returns empty when no user has the e-mail"() {
        given:
        userRepository.findByEmail("nobody@example.com") >> Optional.empty()

        expect:
        service.execute("nobody@example.com").isEmpty()
    }
}
