package com.pi.orders.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(HttpStatus status, String code, String message, String details, Throwable cause) {
        super(status, code, message, details, cause);
    }
}
