package com.algorycarservice.appointment.model;

/** What the customer is coming for; texts live in {@code messages.properties} under {@code appointment.type.*}. */
public enum AppointmentType {
    INSPECTION,
    PRE_INSPECTION,
    MAINTENANCE,
    REPAIR,
    OTHER
}
