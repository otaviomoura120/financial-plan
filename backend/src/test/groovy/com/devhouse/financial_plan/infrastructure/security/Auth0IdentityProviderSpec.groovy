package com.devhouse.financial_plan.infrastructure.security

import com.devhouse.financial_plan.domain.ExternalIdentity
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.gateway.IdentityProvider
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestClient
import spock.lang.Specification

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class Auth0IdentityProviderSpec extends Specification {

    static final String ISSUER = "https://financial-plan.us.auth0.com/"
    static final String USERINFO = "https://financial-plan.us.auth0.com/userinfo"

    RestClient.Builder builder = RestClient.builder()
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build()
    Auth0IdentityProvider provider = new Auth0IdentityProvider(builder, ISSUER)

    def "Spring builds the provider from the issuer-uri property"() {
        given:
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(Auth0IdentityProvider)
                .withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=" + ISSUER)

        expect:
        runner.run { context ->
            assert context.startupFailure == null
            assert context.getBean(IdentityProvider) instanceof Auth0IdentityProvider
        }
    }

    def "fetchIdentity sends the access token to /userinfo and maps the returned claims"() {
        given:
        server.expect(requestTo(USERINFO))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess('{"sub":"auth0|1","email":"joao@example.com","email_verified":true,"name":"João"}',
                        MediaType.APPLICATION_JSON))

        when:
        ExternalIdentity identity = provider.fetchIdentity("token-123")

        then:
        identity.auth0Sub == "auth0|1"
        identity.email == "joao@example.com"
        identity.emailVerified
        server.verify()
    }

    def "fetchIdentity treats a missing email_verified claim as not verified"() {
        given:
        server.expect(requestTo(USERINFO))
                .andRespond(withSuccess('{"sub":"auth0|1","email":"joao@example.com"}', MediaType.APPLICATION_JSON))

        when:
        ExternalIdentity identity = provider.fetchIdentity("token-123")

        then:
        !identity.emailVerified
    }

    def "fetchIdentity builds the /userinfo url when the issuer has no trailing slash"() {
        given:
        RestClient.Builder otherBuilder = RestClient.builder()
        MockRestServiceServer otherServer = MockRestServiceServer.bindTo(otherBuilder).build()
        Auth0IdentityProvider otherProvider = new Auth0IdentityProvider(otherBuilder, "https://financial-plan.us.auth0.com")
        otherServer.expect(requestTo(USERINFO))
                .andRespond(withSuccess('{"sub":"auth0|1"}', MediaType.APPLICATION_JSON))

        when:
        otherProvider.fetchIdentity("token-123")

        then:
        otherServer.verify()
    }

    def "fetchIdentity reports a token rejected by Auth0 as identity_rejected"() {
        given:
        server.expect(requestTo(USERINFO)).andRespond(withStatus(status))

        when:
        provider.fetchIdentity("token-123")

        then:
        DomainException exception = thrown()
        exception.message == "identity_rejected"

        where:
        status << [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "fetchIdentity reports a rate limit or an Auth0 server error as identity_unavailable"() {
        given:
        server.expect(requestTo(USERINFO)).andRespond(withStatus(status))

        when:
        provider.fetchIdentity("token-123")

        then:
        DomainException exception = thrown()
        exception.message == "identity_unavailable"

        where:
        status << [HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE]
    }

    def "fetchIdentity reports an unreachable Auth0 as identity_unavailable"() {
        given:
        server.expect(requestTo(USERINFO)).andRespond(withException(new SocketTimeoutException("timeout")))

        when:
        provider.fetchIdentity("token-123")

        then:
        DomainException exception = thrown()
        exception.message == "identity_unavailable"
    }
}
