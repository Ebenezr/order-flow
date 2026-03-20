package com.blind.orderflow.shared.utils.apis;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ResponseFactory {

    private ResponseFactory() {}

    public static <T> Mono<ApiResponse<T>> success(T body, String requestRefId) {
        return Mono.just(
            new ApiResponse<>(
                new ApiResponseHeader(
                    requestRefId,
                    HttpStatus.OK.value(),
                    "OK",
                    "Success",
                    LocalDateTime.now()
                ),
                body
            )
        );
    }

    public static <T> Mono<ApiResponse<PaginationResponse<T>>> paginated(
            PaginationResponse<T> body,
            String requestRefId
    ) {
        return Mono.just(
                new ApiResponse<>(
                        new ApiResponseHeader(
                                requestRefId,
                                HttpStatus.OK.value(),
                                "OK",
                                "Success",
                                LocalDateTime.now()
                        ),
                        body
                )
        );
    }

    public static <T> Mono<ApiResponse<T>> errorMono(
            HttpStatus status,
            String message,
            String customerMessage,
            String requestRefId
    ) {
        return Mono.just(
            new ApiResponse<>(
                new ApiResponseHeader(
                    requestRefId,
                    status.value(),
                    message,
                    customerMessage,
                    LocalDateTime.now()
                ),
                null
            )
        );
    }

    public static Mono<String> getRequestRefId() {
        return Mono.deferContextual(ctx ->
                Mono.just(ctx.getOrDefault("correlationId", "N/A"))
        );
    }

    public static String newRequestRefId() {
        return UUID.randomUUID().toString();
    }

    // Synchronous error methods for exception handlers
    public static <T> ResponseEntity<ApiResponse<T>> error(
            String errorCode,
            String message,
            String customerMessage,
            HttpStatus status
    ) {
        return ResponseEntity.status(status).body(
            new ApiResponse<>(
                new ApiResponseHeader(
                    newRequestRefId(),
                    status.value(),
                    errorCode + ": " + message,
                    customerMessage,
                    LocalDateTime.now()
                ),
                null
            )
        );
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(
            String errorCode,
            String message,
            HttpStatus status
    ) {
        return error(errorCode, message, message, status);
    }
}
