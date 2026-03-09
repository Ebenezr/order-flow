package com.blind.orderflow.shared.utils.apis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private String errorCode;
    private String message;
    private List<FieldError> fieldErrors;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}