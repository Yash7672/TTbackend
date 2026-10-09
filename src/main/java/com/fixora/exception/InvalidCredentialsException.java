package com.fixora.exception;

/** Wrong email/password combination. Handled as HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
