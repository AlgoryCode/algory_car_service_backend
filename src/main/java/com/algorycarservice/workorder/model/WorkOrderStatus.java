package com.algorycarservice.workorder.model;

/** Life of a vehicle in the workshop: accepted, being worked on, ready for pickup, handed back (or cancelled). */
public enum WorkOrderStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED,
    CANCELLED;

    /** Still occupying the workshop. */
    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS || this == COMPLETED;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == DELIVERED;
    }
}
