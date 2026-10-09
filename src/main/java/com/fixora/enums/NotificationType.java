package com.fixora.enums;

/** In-app notification kinds. Stored in MySQL; no email/SMS provider is configured. */
public enum NotificationType {
    BOOKING_CREATED,
    BOOKING_ACCEPTED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    SERVICE_STARTED,
    SERVICE_COMPLETED,
    COMPLAINT_STATUS_CHANGED,
    REVIEW_REMINDER
}
