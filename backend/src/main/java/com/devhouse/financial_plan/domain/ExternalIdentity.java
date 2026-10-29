package com.devhouse.financial_plan.domain;

import com.devhouse.financial_plan.domain.exception.DomainException;

/**
 * Identity as reported by the identity provider (Auth0), the only trusted source of a user's e-mail:
 * an e-mail typed by the user could belong to someone else and would expose that person's invites.
 */
public class ExternalIdentity {

    private final String auth0Sub;
    private final String email;
    private final boolean emailVerified;

    public ExternalIdentity(String auth0Sub, String email, boolean emailVerified) {
        this.auth0Sub = auth0Sub;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    /**
     * Returns the e-mail only when this identity belongs to the authenticated user and the provider has
     * verified that the user owns the address.
     */
    public String verifiedEmailOf(String authenticatedSub) {
        if (auth0Sub == null || !auth0Sub.equals(authenticatedSub)) {
            throw new DomainException("identity_mismatch");
        }
        if (!emailVerified || email == null || email.isBlank()) {
            throw new DomainException("email_not_verified");
        }
        return email.trim();
    }

    public String getAuth0Sub() { return auth0Sub; }

    public String getEmail() { return email; }

    public boolean isEmailVerified() { return emailVerified; }
}
