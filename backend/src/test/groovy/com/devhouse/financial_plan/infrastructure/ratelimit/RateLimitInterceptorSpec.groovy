package com.devhouse.financial_plan.infrastructure.ratelimit

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification

import java.time.Duration

class RateLimitInterceptorSpec extends Specification {

    RateLimitPolicy policy = new RateLimitPolicy(new RateLimitProperties(true, "100/1m", [
            new RateLimitProperties.Rule("GET", "/spaces/*/members/search", "2/1m")
    ]))
    RateLimitInterceptor interceptor = new RateLimitInterceptor(policy, new RateLimiter(Duration.ofDays(1)))

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private static void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).build()
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt))
    }

    private boolean request(String method, String path, MockHttpServletResponse response = new MockHttpServletResponse()) {
        return interceptor.preHandle(new MockHttpServletRequest(method, path), response, new Object())
    }

    def "lets requests through while the user is under the limit"() {
        given:
        authenticateAs("auth0|ana")

        expect:
        request("GET", "/spaces/1/members/search")
        request("GET", "/spaces/1/members/search")
    }

    def "answers 429 with Retry-After once the user exceeds the endpoint limit"() {
        given:
        authenticateAs("auth0|ana")
        2.times { request("GET", "/spaces/1/members/search") }
        MockHttpServletResponse response = new MockHttpServletResponse()

        when:
        boolean proceeded = request("GET", "/spaces/1/members/search", response)

        then:
        !proceeded
        response.status == 429
        response.getHeader("Retry-After") == "30"
        response.contentAsString == RateLimitInterceptor.TOO_MANY_REQUESTS_MESSAGE
    }

    def "the same Space path in another Space shares the user's bucket"() {
        given:
        authenticateAs("auth0|ana")
        2.times { request("GET", "/spaces/1/members/search") }

        expect:
        !request("GET", "/spaces/2/members/search")
    }

    def "a user over one endpoint limit can still use the other endpoints"() {
        given:
        authenticateAs("auth0|ana")
        2.times { request("GET", "/spaces/1/members/search") }

        expect:
        request("GET", "/transactions")
    }

    def "another user is not affected by the first user's requests"() {
        given:
        authenticateAs("auth0|ana")
        3.times { request("GET", "/spaces/1/members/search") }

        when:
        authenticateAs("auth0|bruno")

        then:
        request("GET", "/spaces/1/members/search")
    }

    def "does not count requests without a JWT"() {
        given:
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))

        expect:
        (1..5).every { request("GET", "/spaces/1/members/search") }
    }
}
