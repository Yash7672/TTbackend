package com.fixora.exception;

/**
 * Thrown when the acting user is not allowed to touch the requested resource
 * (wrong owner, wrong role, or no user identified at all). HTTP 403.
 *
 * NOTE: Fixora has no real authentication yet — the acting user comes from the
 * X-User-Id header, which any client can set. This protects correctness, not
 * security. Documented as a known limitation in the README.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
