package com.devhouse.financial_plan.infrastructure.config;

import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimitInterceptor;
import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimitPolicy;
import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimitProperties;
import com.devhouse.financial_plan.infrastructure.ratelimit.RateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Per-user rate limiting (app.rate-limit.*). RATE_LIMIT_ENABLED=false turns the whole thing off.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public RateLimitConfig(RateLimitProperties properties) {
        RateLimitPolicy policy = new RateLimitPolicy(properties);
        this.rateLimitInterceptor = new RateLimitInterceptor(policy, new RateLimiter(policy.longestPeriod()));
    }

    @Bean
    RateLimitInterceptor rateLimitInterceptor() {
        return rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
    }
}
