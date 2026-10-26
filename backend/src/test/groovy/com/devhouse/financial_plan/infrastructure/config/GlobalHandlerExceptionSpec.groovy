package com.devhouse.financial_plan.infrastructure.config

import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver
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

    def "returns 404 with the message when NotFoundException is thrown"() {
        when:
        ResponseEntity<?> response = handler.handleNotFoundException(new NotFoundException("Transaction not found"), null)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == "Transaction not found"
    }

    def "Spring routes NotFoundException to the 404 handler and other DomainExceptions to the 422 handler"() {
        given:
        ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(GlobalHandlerException)

        expect:
        resolver.resolveMethodByThrowable(new NotFoundException("Transaction not found")).name == "handleNotFoundException"
        resolver.resolveMethodByThrowable(new DomainException("Category not found")).name == "handleDomainException"
    }
}
