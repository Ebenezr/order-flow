package com.blind.orderflow.shared.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super("Access denied", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
}