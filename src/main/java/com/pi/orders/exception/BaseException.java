package com.pi.orders.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final HttpStatus status;
    private final String code;
    private final String details;

    public BaseException(HttpStatus status,
                         String code,
                         String message,
                         String details,
                         Throwable cause) {
        super(message, cause);
        this.status = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        this.code = (code == null || code.isBlank()) ? this.status.name() : code;
        this.details = details;
    }

}
