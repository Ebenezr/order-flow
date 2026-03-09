package com.blind.orderflow.shared.exceptions;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}