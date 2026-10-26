package com.devhouse.financial_plan.domain.exception;

/**
 * The resource addressed by the request (its path) does not exist, or exists in a Space the caller cannot see.
 * Invalid references inside a request body stay as plain {@link DomainException}s, since the addressed URL itself exists.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(final String message) {
        super(message);
    }
}
