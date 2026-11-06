package com.algorycarservice.appointment.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Staff requests start as {@link #REQUESTED} and wait for an admin; admins book straight into {@link #CONFIRMED}.
 * The other three are final.
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Appointments that still hold their slot. */
    public static final Set<AppointmentStatus> ACTIVE = EnumSet.of(REQUESTED, CONFIRMED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
