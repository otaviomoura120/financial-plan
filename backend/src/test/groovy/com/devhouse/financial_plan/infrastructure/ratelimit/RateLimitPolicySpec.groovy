package com.devhouse.financial_plan.infrastructure.ratelimit

import spock.lang.Specification

import java.time.Duration

class RateLimitPolicySpec extends Specification {

    RateLimitPolicy policy = new RateLimitPolicy(new RateLimitProperties(true, "300/1m", [
            new RateLimitProperties.Rule("GET", "/spaces/*/members/search", "10/1m,50/1d"),
            new RateLimitProperties.Rule("POST", "/spaces/*/invites", "10/1h"),
            new RateLimitProperties.Rule("POST", "/spaces", "10/1d"),
            new RateLimitProperties.Rule("POST", "/reports/**", "30/1m")
    ]))

    def "#method #path uses rule '#expectedRule'"() {
        expect:
        policy.resolve(method, path).id() == expectedRule

        where:
        method | path                         | expectedRule
        "GET"  | "/spaces/12/members/search"  | "rule-0 GET /spaces/*/members/search"
        "get"  | "/spaces/12/members/search"  | "rule-0 GET /spaces/*/members/search"
        "POST" | "/spaces/12/invites"         | "rule-1 POST /spaces/*/invites"
        "GET"  | "/spaces/12/invites"         | RateLimitPolicy.DEFAULT_RULE_ID
        "POST" | "/spaces"                    | "rule-2 POST /spaces"
        "PUT"  | "/spaces/12"                 | RateLimitPolicy.DEFAULT_RULE_ID
        "POST" | "/reports"                   | "rule-3 POST /reports/**"
        "POST" | "/reports/by-category"       | "rule-3 POST /reports/**"
        "GET"  | "/transactions"              | RateLimitPolicy.DEFAULT_RULE_ID
    }

    def "parses every limit of a rule with its capacity and period"() {
        when:
        RateLimitRule rule = policy.resolve("GET", "/spaces/1/members/search")

        then:
        rule.limits()*.capacity == [10L, 50L]
        rule.limits()*.refillPeriodNanos == [Duration.ofMinutes(1).toNanos(), Duration.ofDays(1).toNanos()]
    }

    def "keeps buckets for as long as the longest configured period"() {
        expect:
        policy.longestPeriod() == Duration.ofDays(1)
    }

    def "accepts every period unit: #limit"() {
        expect:
        RateLimitPolicy.parseLimits(limit)*.refillPeriodNanos == [period.toNanos()]

        where:
        limit    | period
        "5/30s"  | Duration.ofSeconds(30)
        "5/2m"   | Duration.ofMinutes(2)
        "5/1h"   | Duration.ofHours(1)
        "5/7d"   | Duration.ofDays(7)
        " 5/1h " | Duration.ofHours(1)
    }

    def "refuses a malformed limit at startup: '#limits'"() {
        when:
        new RateLimitPolicy(new RateLimitProperties(true, limits, []))

        then:
        thrown(IllegalStateException)

        where:
        limits << [null, "", "10", "10/m", "ten/1m", "10/1w", "0/1m", "10/0m", "10/1m,"]
    }

    def "refuses a malformed limit in a rule"() {
        when:
        new RateLimitPolicy(new RateLimitProperties(true, "300/1m", [new RateLimitProperties.Rule("GET", "/x", "10 per minute")]))

        then:
        thrown(IllegalStateException)
    }
}
