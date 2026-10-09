package com.fixora.exception;

/** Thrown when a provider is already booked for the requested time window. HTTP 409. */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
