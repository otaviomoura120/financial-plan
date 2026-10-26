package com.devhouse.financial_plan.infrastructure.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Boots Spring's resource server auto-configuration with the issuer and audience declared in application.properties,
 * serving the signing keys from a local JWKS endpoint, to prove which Auth0 access tokens the API accepts.
 */
class JwtValidationSpec extends Specification {

    static final String ISSUER = "https://financial-plan.us.auth0.com/"
    static final String AUDIENCE = "https://financial-plan-api"

    @Shared
    RSAKey signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate()

    @Shared
    HttpServer jwksServer = startJwksServer()

    def cleanupSpec() {
        jwksServer.stop(0)
    }

    private HttpServer startJwksServer() {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        byte[] body = new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8)
        server.createContext("/.well-known/jwks.json") { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
        }
        server.start()
        server
    }

    private List<String> applicationJwtProperties() {
        Properties properties = new Properties()
        getClass().getResourceAsStream("/application.properties").withCloseable { properties.load(it) }
        ["issuer-uri", "audiences"].collect { String key ->
            String name = "spring.security.oauth2.resourceserver.jwt." + key
            name + "=" + properties.getProperty(name)
        }
    }

    private JwtDecoder buildDecoder() {
        JwtDecoder decoder = null
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration, ServletWebSecurityAutoConfiguration,
                        OAuth2ResourceServerAutoConfiguration))
                .withPropertyValues(applicationJwtProperties() as String[])
                .withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=" +
                        "http://localhost:${jwksServer.address.port}/.well-known/jwks.json")
                .run { context -> decoder = context.getBean(JwtDecoder) }
        decoder
    }

    private String token(Map overrides = [:], RSAKey key = signingKey) {
        Map values = [issuer: ISSUER, audience: AUDIENCE, subject: "auth0|123",
                      expiresAt: Instant.now().plusSeconds(300)] + overrides
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(values.issuer as String)
                .audience(values.audience as String)
                .subject(values.subject as String)
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(values.expiresAt as Instant))
                .build()
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
        jwt.sign(new RSASSASigner(key))
        jwt.serialize()
    }

    def "accepts a token from the Auth0 tenant for the API audience and exposes the sub as the authentication name"() {
        when:
        Jwt jwt = buildDecoder().decode(token())

        then:
        jwt.subject == "auth0|123"
        new JwtAuthenticationConverter().convert(jwt).name == "auth0|123"
    }

    def "rejects a token issued for another audience"() {
        when:
        buildDecoder().decode(token(audience: "https://another-api"))

        then:
        thrown(JwtException)
    }

    def "rejects a token from another issuer"() {
        when:
        buildDecoder().decode(token(issuer: "https://attacker.us.auth0.com/"))

        then:
        thrown(JwtException)
    }

    def "rejects an expired token"() {
        when:
        buildDecoder().decode(token(expiresAt: Instant.now().minusSeconds(3600)))

        then:
        thrown(JwtException)
    }

    def "rejects a token signed with a key that is not in the issuer JWKS"() {
        given:
        RSAKey foreignKey = new RSAKeyGenerator(2048).keyID("test-key").generate()

        when:
        buildDecoder().decode(token([:], foreignKey))

        then:
        thrown(JwtException)
    }
}
