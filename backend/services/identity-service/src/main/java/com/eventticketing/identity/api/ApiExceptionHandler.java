package com.eventticketing.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetails> handleApi(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ProblemDetails.of(ex.getStatus(), ex.getCode(), ex.getMessage(), requestId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetails> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ProblemDetails.of(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "Invalid fields: " + detail, requestId(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetails> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ProblemDetails.of(HttpStatus.FORBIDDEN,
                "FORBIDDEN", "You are not authorized for this action.", requestId(request)));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetails> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProblemDetails.of(HttpStatus.UNAUTHORIZED,
                "AUTH_REQUIRED", "Authentication is required.", requestId(request)));
    }

    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            jakarta.validation.ConstraintViolationException.class})
    ResponseEntity<ProblemDetails> handleMalformed(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ProblemDetails.of(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", "The request contains invalid fields.", requestId(request)));
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class})
    ResponseEntity<ProblemDetails> handleConflict(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetails.of(HttpStatus.CONFLICT,
                "CONFLICT", "The resource has changed or already exists. Reload and try again.", requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetails> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR", "An unexpected error occurred.", requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        String id = request.getHeader("X-Request-ID");
        return id == null || id.isBlank() ? "local-request" : id;
    }
}
