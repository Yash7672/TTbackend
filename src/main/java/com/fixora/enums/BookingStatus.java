package com.fixora.enums;

/**
 * Booking lifecycle.
 *
 * Allowed transitions:
 *   PENDING     -> ACCEPTED
 *   PENDING     -> REJECTED
 *   PENDING     -> CANCELLED
 *   ACCEPTED    -> IN_PROGRESS
 *   ACCEPTED    -> CANCELLED
 *   IN_PROGRESS -> COMPLETED
 */
public enum BookingStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
