package com.devhouse.financial_plan.infrastructure.config;

import com.devhouse.financial_plan.domain.exception.DomainException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalHandlerException extends ResponseEntityExceptionHandler {

    protected final Log logger = LogFactory.getLog(this.getClass());

    @ExceptionHandler({DomainException.class})
    public ResponseEntity<?> handleDomainException(DomainException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(exception.getMessage());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<?> handleLockException(ObjectOptimisticLockingFailureException exception, WebRequest request) {
        logger.error(exception);
        return ResponseEntity.status(HttpStatus.LOCKED).body("Registro atualizado ou excluído em outra transação, tente novamente");
    }

    @ExceptionHandler({AuthorizationDeniedException.class})
    public ResponseEntity<?> handleAuthorizationDeniedException(AuthorizationDeniedException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> handleGenericException(Exception exception, WebRequest request) {
        logger.error(exception);
        exception.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error. Consult logs");
    }
}
