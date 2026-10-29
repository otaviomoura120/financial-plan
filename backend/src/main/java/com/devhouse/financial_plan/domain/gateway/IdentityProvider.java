package com.devhouse.financial_plan.domain.gateway;

import com.devhouse.financial_plan.domain.ExternalIdentity;

public interface IdentityProvider {

    /**
     * Asks the identity provider who owns the access token. Throws DomainException when the provider
     * rejects the token or cannot be reached.
     */
    ExternalIdentity fetchIdentity(String accessToken);
}
