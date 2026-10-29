package com.devhouse.financial_plan.infrastructure.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {

    public static RateLimitDecision allow() {
        return new RateLimitDecision(true, Duration.ZERO);
    }

    public static RateLimitDecision reject(Duration retryAfter) {
        return new RateLimitDecision(false, retryAfter);
    }

    /** Retry-After in whole seconds, rounded up so the client never retries too early. */
    public long retryAfterSeconds() {
        long seconds = retryAfter.toSeconds();
        if (retryAfter.toNanosPart() > 0) {
            seconds++;
        }
        return Math.max(seconds, 1);
    }
}
