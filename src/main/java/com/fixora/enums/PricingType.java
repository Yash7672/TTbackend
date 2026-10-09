package com.fixora.enums;

/** How a service or package is priced. */
public enum PricingType {
    /** One fixed, final price for the whole job. */
    FIXED_PRICE,
    /** "Starting from" price; the provider may quote more after inspection. */
    STARTING_PRICE,
    /** A visit/inspection charge only — repair work is billed separately. */
    INSPECTION_FEE,
    /** No price until the provider inspects and submits a quotation. */
    QUOTATION
}
