package com.fixora.exception;

/** Thrown when a booking status change is not allowed (e.g. COMPLETED -> PENDING). HTTP 409. */
public class InvalidBookingTransitionException extends RuntimeException {

    public InvalidBookingTransitionException(String message) {
        super(message);
    }

    public static InvalidBookingTransitionException between(String from, String to) {
        return new InvalidBookingTransitionException(
                "Cannot move a booking from " + from + " to " + to);
    }
}
