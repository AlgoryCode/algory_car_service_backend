package com.algorycarservice.market.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * REQUESTED by the customer, then CONFIRMED or REJECTED by the business. A confirmed booking ends COMPLETED, or
 * CANCELLED by either side while it is still open.
 */
public enum BookingStatus {
    REQUESTED,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    /** Bookings that still hold their slot. */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(REQUESTED, CONFIRMED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
