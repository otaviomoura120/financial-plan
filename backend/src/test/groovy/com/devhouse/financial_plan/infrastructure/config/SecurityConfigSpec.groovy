package com.devhouse.financial_plan.infrastructure.config

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import spock.lang.Specification

class SecurityConfigSpec extends Specification {

    private CorsConfiguration corsFor(String... allowedOrigins) {
        CorsConfigurationSource source = new SecurityConfig(allowedOrigins).corsConfigurationSource()
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/transactions"))
    }

    def "allows only the configured origins, trimming blanks from the comma-separated list"() {
        when:
        CorsConfiguration configuration = corsFor(" https://app.example.com", "https://admin.example.com ", "")

        then:
        configuration.allowedOrigins == ["https://app.example.com", "https://admin.example.com"]
        configuration.checkOrigin("https://app.example.com") == "https://app.example.com"
        configuration.checkOrigin("https://evil.example.com") == null
    }

    def "accepts only the headers the API needs and never browser credentials"() {
        when:
        CorsConfiguration configuration = corsFor("https://app.example.com")

        then:
        configuration.allowedHeaders == ["Authorization", "Content-Type", "X-Space-Id"]
        configuration.checkHeaders(["X-Custom-Header"]) == null
        configuration.allowCredentials == false
    }
}
