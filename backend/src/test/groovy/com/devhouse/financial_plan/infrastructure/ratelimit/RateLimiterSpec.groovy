package com.devhouse.financial_plan.infrastructure.ratelimit

import io.github.bucket4j.TimeMeter
import spock.lang.Specification

import java.time.Duration

class RateLimiterSpec extends Specification {

    ManualClock clock = new ManualClock()
    RateLimiter rateLimiter = new RateLimiter(Duration.ofDays(1), clock)

    RateLimitRule search = new RateLimitRule("search", RateLimitPolicy.parseLimits("3/1m,5/1d"))
    RateLimitRule invites = new RateLimitRule("invites", RateLimitPolicy.parseLimits("3/1m"))

    def "allows requests up to the limit and rejects the next one with the time to wait"() {
        when:
        List<Boolean> allowed = (1..3).collect { rateLimiter.tryConsume("auth0|ana", search).allowed() }
        RateLimitDecision fourth = rateLimiter.tryConsume("auth0|ana", search)

        then:
        allowed == [true, true, true]
        !fourth.allowed()
        fourth.retryAfterSeconds() == 20
    }

    def "each user has a bucket of their own"() {
        given:
        3.times { rateLimiter.tryConsume("auth0|ana", search) }

        expect:
        !rateLimiter.tryConsume("auth0|ana", search).allowed()
        rateLimiter.tryConsume("auth0|bruno", search).allowed()
    }

    def "each rule has a bucket of its own for the same user"() {
        given:
        3.times { rateLimiter.tryConsume("auth0|ana", search) }

        expect:
        !rateLimiter.tryConsume("auth0|ana", search).allowed()
        rateLimiter.tryConsume("auth0|ana", invites).allowed()
    }

    def "the bucket refills over time"() {
        given:
        3.times { rateLimiter.tryConsume("auth0|ana", search) }

        when:
        clock.advance(Duration.ofSeconds(20))

        then:
        rateLimiter.tryConsume("auth0|ana", search).allowed()
        !rateLimiter.tryConsume("auth0|ana", search).allowed()
    }

    def "the daily limit holds even when the per-minute limit has refilled"() {
        given:
        3.times { rateLimiter.tryConsume("auth0|ana", search) }
        clock.advance(Duration.ofMinutes(1))
        2.times { rateLimiter.tryConsume("auth0|ana", search) }

        when:
        clock.advance(Duration.ofMinutes(1))
        RateLimitDecision decision = rateLimiter.tryConsume("auth0|ana", search)

        then:
        !decision.allowed()
        decision.retryAfterSeconds() > 60
    }

    static class ManualClock implements TimeMeter {

        long nanos = 0

        void advance(Duration duration) {
            nanos += duration.toNanos()
        }

        @Override
        long currentTimeNanos() {
            return nanos
        }

        @Override
        boolean isWallClockBased() {
            return false
        }
    }
}
