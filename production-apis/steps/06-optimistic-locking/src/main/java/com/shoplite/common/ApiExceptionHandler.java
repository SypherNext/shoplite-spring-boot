package com.shoplite.common;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 Problem Details responses (application/problem+json).
 * Extending ResponseEntityExceptionHandler gives Problem Details for Spring MVC's own
 * errors too (bad JSON, wrong method, unknown media type), so every error looks the same.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // Someone else changed the same row first (the @Version check failed).
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict",
                "The data was changed by another request. Reload it and try again");
    }

    // Safety net: two requests can pass the service checks at the same moment,
    // and then the database's unique constraint is the one that says no.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violated: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", "The request clashes with existing data");
    }

    // Adds a field-by-field "errors" map so a client can show each message next to its input.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return validationFailed(errors);
    }

    // A constraint directly on a parameter, such as @NotBlank on a header, switches Spring MVC to
    // method validation. Its errors, the @Valid body's included, then arrive here instead, so this
    // method builds the same "errors" map.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors objectErrors) {
                // A @Valid object such as the request body: one entry per invalid field.
                for (FieldError fieldError : objectErrors.getFieldErrors()) {
                    errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
                }
            } else {
                // A single value such as a header: the entry is named after the header.
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    errors.putIfAbsent(nameOf(result.getMethodParameter()), error.getDefaultMessage());
                }
            }
        }
        return validationFailed(errors);
    }

    private static ResponseEntity<Object> validationFailed(Map<String, String> errors) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    // The name the client used: "Idempotency-Key" for a header, the Java parameter name otherwise.
    private static String nameOf(MethodParameter parameter) {
        RequestHeader header = parameter.getParameterAnnotation(RequestHeader.class);
        if (header != null && !header.value().isEmpty()) {
            return header.value();
        }
        if (header != null && !header.name().isEmpty()) {
            return header.name();
        }
        return parameter.getParameterName();
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

}
