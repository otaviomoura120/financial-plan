package com.devhouse.financial_plan.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.time.Duration;

/**
 * Keeps one in-memory bucket per user and rule. Buckets are not shared between instances and start over when
 * the backend restarts; with more than one instance the buckets must move to a shared store (e.g. Redis).
 */
public class RateLimiter {

    private static final long MAX_BUCKETS = 100_000;

    private final Cache<String, Bucket> buckets;
    private final TimeMeter timeMeter;

    public RateLimiter(Duration idleExpiry) {
        this(idleExpiry, TimeMeter.SYSTEM_MILLISECONDS);
    }

    // Receives the clock so tests can advance time and check the refill
    RateLimiter(Duration idleExpiry, TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(idleExpiry)
                .maximumSize(MAX_BUCKETS)
                .build();
    }

    public RateLimitDecision tryConsume(String userKey, RateLimitRule rule) {
        Bucket bucket = buckets.get(rule.id() + "|" + userKey, key -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allow();
        }
        return RateLimitDecision.reject(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    private Bucket newBucket(RateLimitRule rule) {
        LocalBucketBuilder builder = Bucket.builder().withCustomTimePrecision(timeMeter);
        rule.limits().forEach(builder::addLimit);
        return builder.build();
    }
}
