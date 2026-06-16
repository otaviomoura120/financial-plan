package com.devhouse.financial_plan.application

import com.devhouse.financial_plan.application.user.FindUserByAuth0SubService
import com.devhouse.financial_plan.application.user.dto.UserMeResponse
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class FindUserByAuth0SubServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    FindUserByAuth0SubService service = new FindUserByAuth0SubService(userRepository)

    def "execute returns UserMeResponse when user is found"() {
        given:
        String auth0Sub = "auth0|abc123"
        Instant birthdate = Instant.parse("1990-01-01T00:00:00Z")
        User user = new User(1L, 0, auth0Sub, "John Smith", "Johnny", null, null,
                birthdate, "john@example.com", "+5511999999999", true, "Masculino", "Solteiro(a)",
                Instant.now(), null, false)
        userRepository.findByAuth0Sub(auth0Sub) >> user

        when:
        UserMeResponse response = service.execute(auth0Sub)

        then:
        response != null
        response.id() == 1L
        response.name() == "John Smith"
        response.email() == "john@example.com"
        response.nickname() == "Johnny"
        response.phoneNumber() == "+5511999999999"
        response.birthdate() == birthdate
        response.genre() == "Masculino"
        response.maritalStatus() == "Solteiro(a)"
    }

    def "execute returns null when user is not found"() {
        given:
        String auth0Sub = "auth0|unknown"
        userRepository.findByAuth0Sub(auth0Sub) >> null

        when:
        UserMeResponse response = service.execute(auth0Sub)

        then:
        response == null
    }
}
