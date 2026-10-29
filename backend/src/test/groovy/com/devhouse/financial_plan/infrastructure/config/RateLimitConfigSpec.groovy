package com.devhouse.financial_plan.infrastructure.config

import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimitInterceptor
import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimitProperties
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import spock.lang.Specification

/**
 * Loads the real application.properties, so a typo in app.rate-limit.* fails here instead of at deploy time.
 */
class RateLimitConfigSpec extends Specification {

    WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(RateLimitConfig)

    def "binds the configured rules and registers the interceptor"() {
        expect:
        runner.run { context ->
            assert context.startupFailure == null
            assert context.getBean(RateLimitInterceptor) != null

            RateLimitProperties properties = context.getBean(RateLimitProperties)
            assert properties.defaultLimits() == "300/1m"
            assert properties.rules()*.path == ["/spaces/*/members/search", "/spaces/*/invites", "/spaces", "/users", "/reports/**"]
            assert properties.rules()[0].limits() == "10/1m,50/1d"
        }
    }

    def "environment variables override the limits"() {
        expect:
        runner.withPropertyValues("RATE_LIMIT_MEMBER_SEARCH=3/1m").run { context ->
            assert context.getBean(RateLimitProperties).rules()[0].limits() == "3/1m"
        }
    }

    def "a malformed limit stops the application at startup"() {
        expect:
        runner.withPropertyValues("RATE_LIMIT_DEFAULT=300 per minute").run { context ->
            assert context.startupFailure != null
        }
    }

    def "RATE_LIMIT_ENABLED=false turns the rate limit off"() {
        expect:
        runner.withPropertyValues("RATE_LIMIT_ENABLED=false").run { context ->
            assert context.getBeansOfType(RateLimitInterceptor).isEmpty()
        }
    }
}
