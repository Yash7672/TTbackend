package com.fixora.exception;

/** Thrown when a request is well-formed but violates a business rule. Handled as HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
