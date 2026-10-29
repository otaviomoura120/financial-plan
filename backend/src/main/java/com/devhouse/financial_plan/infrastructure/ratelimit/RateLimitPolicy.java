package com.devhouse.financial_plan.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves which limits apply to a request. The configuration is parsed once, so a malformed limit
 * stops the application at startup instead of failing on the first request.
 */
public class RateLimitPolicy {

    static final String DEFAULT_RULE_ID = "default";

    private static final Pattern LIMIT_FORMAT = Pattern.compile("(\\d+)/(\\d+)([smhd])");

    private final List<MatchingRule> rules;
    private final RateLimitRule defaultRule;

    public RateLimitPolicy(RateLimitProperties properties) {
        this.defaultRule = new RateLimitRule(DEFAULT_RULE_ID, parseLimits(properties.defaultLimits()));
        this.rules = compileRules(properties.rules());
    }

    /** The first configured rule matching method and path, or the default rule. */
    public RateLimitRule resolve(String method, String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        return rules.stream()
                .filter(rule -> rule.matches(method, pathContainer))
                .map(MatchingRule::rule)
                .findFirst()
                .orElse(defaultRule);
    }

    /** How long a bucket must be kept after its last use: an idle bucket older than this is full again anyway. */
    public Duration longestPeriod() {
        return allRules().stream()
                .flatMap(rule -> rule.limits().stream())
                .map(limit -> Duration.ofNanos(limit.getRefillPeriodNanos()))
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    private List<RateLimitRule> allRules() {
        List<RateLimitRule> all = new ArrayList<>(rules.stream().map(MatchingRule::rule).toList());
        all.add(defaultRule);
        return all;
    }

    private static List<MatchingRule> compileRules(List<RateLimitProperties.Rule> configuredRules) {
        PathPatternParser parser = new PathPatternParser();
        List<MatchingRule> compiled = new ArrayList<>();
        for (int index = 0; index < configuredRules.size(); index++) {
            RateLimitProperties.Rule rule = configuredRules.get(index);
            String id = "rule-" + index + " " + rule.method() + " " + rule.path();
            compiled.add(new MatchingRule(rule.method(), parser.parse(rule.path()),
                    new RateLimitRule(id, parseLimits(rule.limits()))));
        }
        return compiled;
    }

    static List<Bandwidth> parseLimits(String limits) {
        if (limits == null || limits.isBlank()) {
            throw new IllegalStateException("Rate limit cannot be empty");
        }
        // -1 keeps a trailing empty item, so "10/1m," is refused instead of silently accepted
        return Arrays.stream(limits.split(",", -1))
                .map(String::trim)
                .map(RateLimitPolicy::parseLimit)
                .toList();
    }

    private static Bandwidth parseLimit(String limit) {
        Matcher matcher = LIMIT_FORMAT.matcher(limit);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid rate limit '" + limit + "', expected <requests>/<period>, e.g. 10/1m");
        }
        long requests = Long.parseLong(matcher.group(1));
        Duration period = toDuration(Long.parseLong(matcher.group(2)), matcher.group(3));
        if (requests == 0 || period.isZero()) {
            throw new IllegalStateException("Invalid rate limit '" + limit + "', requests and period must be positive");
        }
        return Bandwidth.builder().capacity(requests).refillGreedy(requests, period).build();
    }

    private static Duration toDuration(long amount, String unit) {
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
    }

    private record MatchingRule(String method, PathPattern pattern, RateLimitRule rule) {

        boolean matches(String requestMethod, PathContainer path) {
            return method.equalsIgnoreCase(requestMethod) && pattern.matches(path);
        }
    }
}
