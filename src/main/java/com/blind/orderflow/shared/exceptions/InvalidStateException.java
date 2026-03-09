package com.blind.orderflow.shared.exceptions;

public class InvalidStateException extends BusinessException {

    public InvalidStateException(String message) {
        super(message, "INVALID_STATE");
    }
}