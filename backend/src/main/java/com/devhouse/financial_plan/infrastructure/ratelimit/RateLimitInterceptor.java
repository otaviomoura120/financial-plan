package com.devhouse.financial_plan.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Counts every authenticated request against the rule of its endpoint, keyed by the JWT subject.
 * Runs after the security filter chain, so unauthenticated requests never reach it (they get 401 first).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    static final String TOO_MANY_REQUESTS_MESSAGE = "Too many requests, try again later";

    private final Log logger = LogFactory.getLog(this.getClass());
    private final RateLimitPolicy policy;
    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimitPolicy policy, RateLimiter rateLimiter) {
        this.policy = policy;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            return true;
        }

        RateLimitRule rule = policy.resolve(request.getMethod(), request.getRequestURI());
        RateLimitDecision decision = rateLimiter.tryConsume(authentication.getName(), rule);
        if (decision.allowed()) {
            return true;
        }

        logger.warn("Rate limit '" + rule.id() + "' exceeded by " + authentication.getName());
        reject(response, decision);
        return false;
    }

    private void reject(HttpServletResponse response, RateLimitDecision decision) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(TOO_MANY_REQUESTS_MESSAGE);
    }
}
