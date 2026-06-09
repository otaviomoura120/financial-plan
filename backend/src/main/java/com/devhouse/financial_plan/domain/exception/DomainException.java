package com.devhouse.financial_plan.domain.exception;

public class DomainException extends NoStacktraceException {

    public DomainException(final String message) {
        super(message);
    }
}
