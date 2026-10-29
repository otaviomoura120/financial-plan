package com.devhouse.financial_plan.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * app.rate-limit.* from application.properties. Limits are written as "10/1m,50/1d" and parsed by RateLimitPolicy.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, String defaultLimits, List<Rule> rules) {

    public RateLimitProperties {
        if (rules == null) {
            rules = List.of();
        }
    }

    public record Rule(String method, String path, String limits) {}
}
