package com.blind.orderflow.shared.exceptions;


import com.blind.orderflow.shared.utils.apis.ApiResponse;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        return ResponseFactory.error(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getStatus()
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseFactory.error(
                "ACCESS_DENIED",
                ex.getMessage(),
                org.springframework.http.HttpStatus.FORBIDDEN
        );
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<?>> handleBadRequest(IllegalArgumentException ex) {

        return Mono.just(
                ResponseEntity.badRequest().body(
                        Map.of(
                                "error", "BAD_REQUEST",
                                "message", ex.getMessage()
                        )
                )
            );
        }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseFactory.error(
                "ACCESS_DENIED",
                ex.getMessage(),
                org.springframework.http.HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseFactory.error(
                "VALIDATION_ERROR",
                message,
                org.springframework.http.HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Object>> handleWebExchangeBindException(WebExchangeBindException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseFactory.error(
                "VALIDATION_ERROR",
                message,
                org.springframework.http.HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(IllegalStateException ex) {
        return ResponseFactory.error(
                "CONFLICT",
                ex.getMessage(),
                org.springframework.http.HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnhandled(Exception ex) {
        log.error("Unhandled exception: ", ex);

        return ResponseFactory.error(
                "INTERNAL_ERROR",
                "Unexpected error occurred",
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}