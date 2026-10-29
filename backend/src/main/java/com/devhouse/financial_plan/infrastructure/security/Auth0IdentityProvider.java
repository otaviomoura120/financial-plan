package com.devhouse.financial_plan.infrastructure.security;

import com.devhouse.financial_plan.domain.ExternalIdentity;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.gateway.IdentityProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Reads the user's identity from Auth0's /userinfo endpoint. The access token must have been issued with the
 * "openid email" scopes (the Nuxt SDK requests "openid profile email offline_access" by default).
 */
@Component
public class Auth0IdentityProvider implements IdentityProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<Map<String, Object>> CLAIMS = new ParameterizedTypeReference<>() {};

    private final Log logger = LogFactory.getLog(this.getClass());
    private final RestClient restClient;

    @Autowired
    public Auth0IdentityProvider(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        this(RestClient.builder().requestFactory(timeoutRequestFactory()), issuerUri);
    }

    // Receives the builder so tests can bind a MockRestServiceServer to it
    Auth0IdentityProvider(RestClient.Builder builder, String issuerUri) {
        this.restClient = builder
                .baseUrl(userInfoUrl(issuerUri))
                .build();
    }

    @Override
    public ExternalIdentity fetchIdentity(String accessToken) {
        try {
            Map<String, Object> claims = restClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(CLAIMS);
            return toIdentity(claims);
        } catch (RestClientResponseException exception) {
            throw responseFailure(exception);
        } catch (RestClientException exception) {
            logger.error("Auth0 /userinfo could not be reached", exception);
            throw new DomainException("identity_unavailable");
        }
    }

    // 401/403 mean Auth0 refused this token; anything else (rate limit, 5xx) is Auth0 being unavailable
    private DomainException responseFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            logger.warn("Auth0 /userinfo rejected the access token with status " + status);
            return new DomainException("identity_rejected");
        }
        logger.error("Auth0 /userinfo failed with status " + status, exception);
        return new DomainException("identity_unavailable");
    }

    private ExternalIdentity toIdentity(Map<String, Object> claims) {
        if (claims == null) {
            throw new DomainException("identity_rejected");
        }
        Object sub = claims.get("sub");
        Object email = claims.get("email");
        boolean emailVerified = Boolean.TRUE.equals(claims.get("email_verified"));
        return new ExternalIdentity(sub instanceof String ? (String) sub : null,
                email instanceof String ? (String) email : null, emailVerified);
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return requestFactory;
    }

    private static String userInfoUrl(String issuerUri) {
        if (issuerUri.endsWith("/")) {
            return issuerUri + "userinfo";
        }
        return issuerUri + "/userinfo";
    }
}
