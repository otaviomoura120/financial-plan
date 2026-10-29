package com.devhouse.financial_plan.application.user

import com.devhouse.financial_plan.application.user.dto.CreateUserRequest
import com.devhouse.financial_plan.application.user.dto.UserResponse
import com.devhouse.financial_plan.domain.ExternalIdentity
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.gateway.IdentityProvider
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class CreateUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    IdentityProvider identityProvider = Mock()
    CreateUserService service = new CreateUserService(userRepository, identityProvider)

    CreateUserRequest request = new CreateUserRequest("João Silva", "Jo", null, null, null, "+55 11 99999-9999", null, null)

    def "execute saves the user with the e-mail verified by the identity provider"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> null
        identityProvider.fetchIdentity("token") >> new ExternalIdentity("auth0|1", "joao@example.com", true)
        userRepository.findByEmail("joao@example.com") >> Optional.empty()

        when:
        UserResponse response = service.execute("auth0|1", "token", request)

        then:
        1 * userRepository.save({ User user ->
            user.auth0Sub == "auth0|1" && user.email == "joao@example.com" && user.name == "João Silva" &&
                    user.phoneNumber == "+55 11 99999-9999" && user.active && !user.masterAdmin
        }) >> { User user -> user.id = 10L; user }

        and:
        response == new UserResponse(10L, 0, "João Silva")
    }

    def "execute does not save a user whose e-mail is not verified"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> null
        identityProvider.fetchIdentity("token") >> new ExternalIdentity("auth0|1", "joao@example.com", false)

        when:
        service.execute("auth0|1", "token", request)

        then:
        DomainException exception = thrown()
        exception.message == "email_not_verified"
        0 * userRepository.save(_)
    }

    def "execute does not save a user when the identity belongs to someone else"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> null
        identityProvider.fetchIdentity("token") >> new ExternalIdentity("auth0|2", "maria@example.com", true)

        when:
        service.execute("auth0|1", "token", request)

        then:
        DomainException exception = thrown()
        exception.message == "identity_mismatch"
        0 * userRepository.save(_)
    }

    def "execute rejects an e-mail that already belongs to another account"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> null
        identityProvider.fetchIdentity("token") >> new ExternalIdentity("auth0|1", "joao@example.com", true)
        userRepository.findByEmail("joao@example.com") >> Optional.of(existingUser("auth0|2"))

        when:
        service.execute("auth0|1", "token", request)

        then:
        DomainException exception = thrown()
        exception.message == "email_already_registered"
        0 * userRepository.save(_)
    }

    def "execute rejects a second registration without calling the identity provider"() {
        given:
        userRepository.findByAuth0Sub("auth0|1") >> existingUser("auth0|1")

        when:
        service.execute("auth0|1", "token", request)

        then:
        DomainException exception = thrown()
        exception.message == "user_already_registered"
        0 * identityProvider.fetchIdentity(_)
        0 * userRepository.save(_)
    }

    private static User existingUser(String auth0Sub) {
        return new User(1L, 0, auth0Sub, "Outro", null, null, null, null, "joao@example.com", null, true,
                null, null, Instant.now(), null, false)
    }
}
