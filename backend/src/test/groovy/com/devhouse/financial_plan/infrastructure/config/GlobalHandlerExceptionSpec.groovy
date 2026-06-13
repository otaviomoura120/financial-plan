package com.devhouse.financial_plan.infrastructure.config

import com.devhouse.financial_plan.domain.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.authorization.AuthorizationDeniedException
import spock.lang.Specification

class GlobalHandlerExceptionSpec extends Specification {

    GlobalHandlerException handler = new GlobalHandlerException()

    def "returns 403 when AuthorizationDeniedException is thrown"() {
        given:
        AuthorizationDeniedException exception = new AuthorizationDeniedException("Access Denied")

        when:
        ResponseEntity<?> response = handler.handleAuthorizationDeniedException(exception, null)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body == "Access denied"
    }

    def "returns 422 when DomainException is thrown"() {
        given:
        DomainException exception = new DomainException("invalid field")

        when:
        ResponseEntity<?> response = handler.handleDomainException(exception, null)

        then:
        response.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        response.body == "invalid field"
    }

    def "returns 423 when ObjectOptimisticLockingFailureException is thrown"() {
        given:
        ObjectOptimisticLockingFailureException exception = new ObjectOptimisticLockingFailureException("Entity", 1L)

        when:
        ResponseEntity<?> response = handler.handleLockException(exception, null)

        then:
        response.statusCode == HttpStatus.LOCKED
    }

    def "returns 500 when a generic Exception is thrown"() {
        given:
        Exception exception = new RuntimeException("unexpected error")

        when:
        ResponseEntity<?> response = handler.handleGenericException(exception, null)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
