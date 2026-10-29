package com.devhouse.financial_plan.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;

import java.util.List;

/**
 * A set of limits that share one bucket per user. The id keeps the buckets of different rules apart.
 */
public record RateLimitRule(String id, List<Bandwidth> limits) {}
