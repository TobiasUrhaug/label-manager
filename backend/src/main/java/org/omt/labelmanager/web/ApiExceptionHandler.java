package org.omt.labelmanager.web;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders exceptions that cross a controller boundary as RFC 9457 ProblemDetail responses.
 *
 * <p>Replaces the Thymeleaf-era handler that returned view names ({@code "error/404"}) after the
 * template engine was removed, leaving those responses with an empty body and no content type.
 *
 * <p>Controller-local {@code @ExceptionHandler} methods take precedence over this advice; it
 * supplies the default for controllers that do not declare their own.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException exception) {
        log.debug("Resource not found: {}", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ProblemDetail handleInsufficientInventory(InsufficientInventoryException exception) {
        log.debug("Insufficient inventory: {}", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ProblemDetail handleBadRequest(RuntimeException exception) {
        log.debug("Rejected request: {}", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
