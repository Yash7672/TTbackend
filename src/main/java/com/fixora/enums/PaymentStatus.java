package com.fixora.enums;

/** Payment lifecycle. Fixora records intent only — no payment gateway is wired up. */
public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    FAILED
}
