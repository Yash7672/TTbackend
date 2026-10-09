package com.fixora.exception;

/** Thrown on a unique-constraint violation such as a duplicate email. Handled as HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
